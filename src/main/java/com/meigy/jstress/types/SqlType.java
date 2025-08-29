package com.meigy.jstress.types;

/**
 * @Author Meigy
 * @Description 对象还没有描述
 * @Date 2025-08-29 14:53
 **/
public enum SqlType {
    QUERY,      // SELECT
    UPDATE,     // INSERT, UPDATE, DELETE
    CALL,       // 存储过程
    EXECUTE     // 其他
}
