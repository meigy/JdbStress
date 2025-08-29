package com.meigy.jstress.core;

import com.meigy.jstress.properties.StressProperties;
import com.meigy.jstress.types.SqlType;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

//@Component
public class SqlGenerator {
    private final Logger logger = LoggerFactory.getLogger(UserContext.class);
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+.*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*(UPDATE|INSERT|DELETE)\\s+.*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern CALL_PATTERN = Pattern.compile("^\\s*(CALL|EXEC)\\s+.*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private SqlExecutor sqlExecutor;
    private String sql;
    private List<String[]> params;
    private SqlType sqlType;
    private final AtomicInteger currentParamIndex = new AtomicInteger(0);
    //private StressProperties properties;
    private final CustomStringParser parser;
    public SqlGenerator() {
        UserContext userContext = AppContextHolder.getBean(UserContext.class);
        logger.warn("当前数据源:" + userContext.getDatasourceName());
        this.sqlExecutor = SqlExecutor.getInstance(userContext.getDatasourceName());
        this.sql = userContext.getStressSqlTemplate();
        this.params = userContext.getStressSqlParams();
        this.parser = new CustomStringParser(this.sql);
        this.analyzeSql();
    }

    //@PostConstruct
//    public void init() {
//        try {
//            //loadSql(properties.getSql().getFilePath());
//            //loadParams(properties.getSql().getParamsPath());
//            analyzeSql();
//        } catch (Exception e) {
//            logger.error("初始化SQL加载器失败", e);
//        }
//    }

    public void reLoad() {
        analyzeSql();
    }

//    public void loadSql(String sqlPath) throws Exception {
//        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        Resource resource = resolver.getResource(sqlPath);
//
//        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
//            sql = FileCopyUtils.copyToString(reader);
//        }
//
//        if (sql == null || sql.isEmpty()) {
//            throw new IllegalArgumentException("SQL文件为空");
//        }
//
//    }
//
//    public void loadParams(String paramsPath) throws Exception {
//        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        Resource resource = resolver.getResource(paramsPath);
//
//        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
//             CSVReader csvReader = new CSVReader(reader)) {
//            params = csvReader.readAll();
//        }
//    }

    public void analyzeSql() {
        //sqlExecutor.analyzeSql(sql);
        String upperSql = sql.trim().toUpperCase();
        if (SELECT_PATTERN.matcher(upperSql).matches()) {
            sqlType = SqlType.QUERY;
        } else if (UPDATE_PATTERN.matcher(upperSql).matches()) {
            sqlType = SqlType.UPDATE;
        } else if (CALL_PATTERN.matcher(upperSql).matches()) {
            sqlType = SqlType.CALL;
        } else {
            sqlType = SqlType.EXECUTE;
        }
        logger.info("SQL类型识别为: {}", sqlType);
    }


    /*
    定义:pt表示用当前一个时间戳生成一个长整数，并用这个数替换掉字符串中所有的:pt;
    定义:ps表示用当前一个时间戳生成一个字符串并拼接一个随机数，并用这个字符串替换掉字符串中所有的:ps;
    定义:pr表示生成一个随机的正整数，执行结果是生成一个正整数，并用这个数替换掉字符串中所有的:pr;
    定义:PR表示生成一个随机的正整数，执行结果是生成一个正整数，并用这个数替换掉字符串中一个:PR，如何后面还有:PR需再生成一个随机数替换下一个:PR;
    定义:pi[100]表示用一个自增数发生器（从100开始的整数发生器）的下一个值，替换掉字符串中所有的:pi[100]
    定义:PI[100]表示用一个自增数发生器（从100开始的整数发生器）的下一个值，替换掉字符串中一个:PI[100]，如何后面还有:PI[100]需再取下一个值替换下一个PI[100];
    定义:pl[1000]表示用一个自增数发生器（从1000开始的长整数发生器）的下一个值，替换掉字符串中所有的:pl[1000]
    定义:PL[1000]表示用一个自增数发生器（从1000开始的长整数发生器）的下一个值，替换掉字符串中一个:PL[1000]，如何后面还有:PL[1000]需再取下一个值替换下一个PL[1000];
     */
    /*
    :p1,:p2,:p3 表示指定值的替换
     */
    public void executeSql() {
        String[] currentParams = getNextParams();
        //sqlExecutor.execute(sql, currentParams);
        sqlExecutor.execute(parser.getNext(), currentParams, sqlType);
    }

    public void switchDataSource(String dataSourceName) {
        //sqlExecutor.switchDataSource();
        sqlExecutor = SqlExecutor.getInstance(dataSourceName);
    }

    private String[] getNextParams() {
        if (params == null || params.isEmpty()) {
            return new String[0];
        }
        int index = currentParamIndex.getAndIncrement() % params.size();
        return params.get(index);
    }

    public String getSql() {
        return sql;
    }
} 