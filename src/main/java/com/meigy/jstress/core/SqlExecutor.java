package com.meigy.jstress.core;

import com.meigy.jstress.types.SqlType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

@Slf4j
//@Component
public class SqlExecutor {
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    private final JdbcTemplateManager jdbcTemplateManager;
    
    // SQL类型的正则表达式
//    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+.*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
//    private static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*(UPDATE|INSERT|DELETE)\\s+.*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
//    private static final Pattern CALL_PATTERN = Pattern.compile("^\\s*(CALL|EXEC)\\s+.*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private volatile long executeCounter = 0;
    
    // SQL类型枚举

    public static SqlExecutor getInstance(String dataSourceName) {
        SqlExecutor executor = new SqlExecutor();
        executor.switchDataSource(dataSourceName);
        return executor;
    }

    public SqlExecutor() {
        UserContext userContext = AppContextHolder.getBean(UserContext.class);
        JdbcTemplateManager jdbcTemplateManager = AppContextHolder.getBean(JdbcTemplateManager.class);
        this.jdbcTemplateManager = jdbcTemplateManager;
        this.namedJdbcTemplate = userContext.getActiveNamedJdbcTemplate();
    }

    private void switchDataSource(String dataSourceName) {
        this.namedJdbcTemplate = jdbcTemplateManager.getNamedJdbcTemplate(dataSourceName);;
    }

    /**
     * 分析SQL类型
     */
//    public void analyzeSql(String sql) {
//        String upperSql = sql.trim().toUpperCase();
//        if (SELECT_PATTERN.matcher(upperSql).matches()) {
//            sqlType = SqlType.QUERY;
//        } else if (UPDATE_PATTERN.matcher(upperSql).matches()) {
//            sqlType = SqlType.UPDATE;
//        } else if (CALL_PATTERN.matcher(upperSql).matches()) {
//            sqlType = SqlType.CALL;
//        } else {
//            sqlType = SqlType.EXECUTE;
//        }
//        log.info("SQL类型识别为: {}", sqlType);
//    }

    /**
     * 执行SQL
     * @param sql 包含命名参数的SQL（如:p1, :p2）
     * @param params 参数值数组
     */
    public void execute(String sql, String[] params, SqlType sqlType) {
        try {
            //if (params.length == 0) {
            //    sql = preProcessSql(sql);
            //}
            if (executeCounter++ % 1000 == 0) {
                log.info(sql);
            }
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
            // 添加时间戳参数
            //paramSource.addValue("pt", System.currentTimeMillis());
            // 添加随机数参数
            //paramSource.addValue("pr", new Random().nextInt());
        }
        return paramSource;
    }

    private String preProcessSql(String sql) {
        String result = sql;
//        if (result.contains(":pt")) {
//            result = result.replaceAll(":pt", String.valueOf(System.currentTimeMillis()));
//        }
//        //获取result字符串中满足:pr\d+的部分
//
//        if (result.contains(":pr")) {
//
//        }
//        result = result.replaceAll(":pr", String.valueOf(new Random().nextInt()));
        return result;
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