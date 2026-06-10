package com.cashier.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 行映射器接口
 * 用于将 ResultSet 映射为对象
 * @param <T> 映射的目标类型
 */
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
