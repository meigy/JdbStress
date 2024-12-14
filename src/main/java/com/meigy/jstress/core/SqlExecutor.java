package com.meigy.jstress.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import com.meigy.jstress.config.DataSourceConfig;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SqlExecutor {
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    private final JdbcTemplateManager jdbcTemplateManager;
    
    // SQL类型的正则表达式
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*(UPDATE|INSERT|DELETE)\\s+.*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CALL_PATTERN = Pattern.compile("^\\s*(CALL|EXEC)\\s+.*$", Pattern.CASE_INSENSITIVE);
    
    // SQL类型枚举
    public enum SqlType {
        QUERY,      // SELECT
        UPDATE,     // INSERT, UPDATE, DELETE
        CALL,       // 存储过程
        EXECUTE     // 其他
    }

    private SqlType sqlType;

    public SqlExecutor(JdbcTemplateManager jdbcTemplateManager) {
        this.jdbcTemplateManager = jdbcTemplateManager;
        switchDataSource();
    }

    public void switchDataSource() {
        this.namedJdbcTemplate = jdbcTemplateManager.createNamedJdbcTemplate();;
    }

    /**
     * 分析SQL类型
     */
    public void analyzeSql(String sql) {
        if (SELECT_PATTERN.matcher(sql).matches()) {
            sqlType = SqlType.QUERY;
        } else if (UPDATE_PATTERN.matcher(sql).matches()) {
            sqlType = SqlType.UPDATE;
        } else if (CALL_PATTERN.matcher(sql).matches()) {
            sqlType = SqlType.CALL;
        } else {
            sqlType = SqlType.EXECUTE;
        }
        log.info("SQL类型识别为: {}", sqlType);
    }

    /**
     * 执行SQL
     * @param sql 包含命名参数的SQL（如:p1, :p2）
     * @param params 参数值数组
     */
    public void execute(String sql, String[] params) {
        try {
            MapSqlParameterSource paramSource = createParamSource(params);
            switch (sqlType) {
                case QUERY:
                    executeQuery(sql, paramSource);
                    break;
                case UPDATE:
                    executeUpdate(sql, paramSource);
                    break;
                case CALL:
                    executeCall(sql, paramSource);
                    break;
                case EXECUTE:
                    executeGeneral(sql, paramSource);
                    break;
            }
        } catch (Exception e) {
            log.error("SQL执行失败: {}", e.getMessage());
            throw e;
        }
    }

    private MapSqlParameterSource createParamSource(String[] params) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                paramSource.addValue("p" + (i + 1), params[i]);
            }
        }
        return paramSource;
    }

    private void executeQuery(String sql, MapSqlParameterSource params) {
        List<Map<String, Object>> result = namedJdbcTemplate.queryForList(sql, params);
        log.debug("查询返回 {} 条记录", result.size());
    }

    private void executeUpdate(String sql, MapSqlParameterSource params) {
        int affected = namedJdbcTemplate.update(sql, params);
        log.debug("更新影响 {} 条记录", affected);
    }

    private void executeCall(String sql, MapSqlParameterSource params) {
        namedJdbcTemplate.execute(sql, params, ps -> ps.execute());
    }

    private void executeGeneral(String sql, MapSqlParameterSource params) {
        namedJdbcTemplate.execute(sql, params, ps -> ps.execute());
    }
} 