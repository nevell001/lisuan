package com.cashier.dao;

import com.cashier.model.Supplier;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 供应商数据访问对象
 * 负责供应商相关的数据库操作
 */
public class SupplierDAO {
    private static final Logger logger = LoggerFactory.getLogger(SupplierDAO.class);

    /**
     * 根据ID查找供应商
     *
     * @param id 供应商ID
     * @return 供应商对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static Supplier findById(int id) throws SQLException {
        String sql = "SELECT id, supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time " +
                     "FROM suppliers WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToSupplier(rs);
            }
        }
        return null;
    }

    /**
     * 查询所有供应商
     *
     * @return 供应商列表
     * @throws SQLException 数据库操作异常
     */
    public static List<Supplier> findAll() throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT id, supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time " +
                     "FROM suppliers ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                suppliers.add(mapRowToSupplier(rs));
            }
        }
        return suppliers;
    }

    /**
     * 根据供应商编号查找供应商
     *
     * @param supplierCode 供应商编号
     * @return 供应商对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public static Supplier findByCode(String supplierCode) throws SQLException {
        String sql = "SELECT id, supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time " +
                     "FROM suppliers WHERE supplier_code = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, supplierCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToSupplier(rs);
            }
        }
        return null;
    }

    /**
     * 根据供应商名称查找供应商
     *
     * @param name 供应商名称
     * @return 供应商列表
     * @throws SQLException 数据库操作异常
     */
    public static List<Supplier> findByName(String name) throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT id, supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time " +
                     "FROM suppliers WHERE name LIKE ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                suppliers.add(mapRowToSupplier(rs));
            }
        }
        return suppliers;
    }

    /**
     * 根据供应商等级查找供应商
     *
     * @param rank 供应商等级（A、B、C）
     * @return 供应商列表
     * @throws SQLException 数据库操作异常
     */
    public static List<Supplier> findByRank(String rank) throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT id, supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time " +
                     "FROM suppliers WHERE `rank` = ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, rank);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                suppliers.add(mapRowToSupplier(rs));
            }
        }
        return suppliers;
    }

    /**
     * 根据状态查找供应商
     *
     * @param status 状态（true-启用，false-禁用）
     * @return 供应商列表
     * @throws SQLException 数据库操作异常
     */
    public static List<Supplier> findByStatus(boolean status) throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        String sql = "SELECT id, supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time " +
                     "FROM suppliers WHERE status = ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBoolean(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                suppliers.add(mapRowToSupplier(rs));
            }
        }
        return suppliers;
    }

    /**
     * 插入新供应商
     *
     * @param supplier 供应商对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean insert(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO suppliers (supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, supplier.supplierCode);
            pstmt.setString(2, supplier.name);
            pstmt.setString(3, supplier.contactPerson);
            pstmt.setString(4, supplier.phone);
            pstmt.setString(5, supplier.address);
            pstmt.setString(6, supplier.rank);
            pstmt.setBoolean(7, supplier.status);
            pstmt.setString(8, supplier.remark);
            pstmt.setTimestamp(9, supplier.createTime);
            pstmt.setTimestamp(10, supplier.updateTime);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        supplier.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新供应商信息
     *
     * @param supplier 供应商对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean update(Supplier supplier) throws SQLException {
        String sql = "UPDATE suppliers SET supplier_code = ?, name = ?, contact_person = ?, phone = ?, " +
                     "address = ?, `rank` = ?, status = ?, remark = ?, update_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, supplier.supplierCode);
            pstmt.setString(2, supplier.name);
            pstmt.setString(3, supplier.contactPerson);
            pstmt.setString(4, supplier.phone);
            pstmt.setString(5, supplier.address);
            pstmt.setString(6, supplier.rank);
            pstmt.setBoolean(7, supplier.status);
            pstmt.setString(8, supplier.remark);
            pstmt.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(10, supplier.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除供应商
     *
     * @param id 供应商ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM suppliers WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入供应商
     *
     * @param suppliers 供应商列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsert(List<Supplier> suppliers) throws SQLException {
        String sql = "INSERT INTO suppliers (supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Supplier supplier : suppliers) {
                pstmt.setString(1, supplier.supplierCode);
                pstmt.setString(2, supplier.name);
                pstmt.setString(3, supplier.contactPerson);
                pstmt.setString(4, supplier.phone);
                pstmt.setString(5, supplier.address);
                pstmt.setString(6, supplier.rank);
                pstmt.setBoolean(7, supplier.status);
                pstmt.setString(8, supplier.remark);
                pstmt.setTimestamp(9, supplier.createTime);
                pstmt.setTimestamp(10, supplier.updateTime);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Supplier 对象
     *
     * @param rs 结果集
     * @return 供应商对象
     * @throws SQLException 数据库操作异常
     */
    private static Supplier mapRowToSupplier(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.id = rs.getInt("id");
        supplier.supplierCode = rs.getString("supplier_code");
        supplier.name = rs.getString("name");
        supplier.contactPerson = rs.getString("contact_person");
        supplier.phone = rs.getString("phone");
        supplier.address = rs.getString("address");
        supplier.rank = rs.getString("rank");
        supplier.status = rs.getBoolean("status");
        supplier.remark = rs.getString("remark");
        supplier.createTime = rs.getTimestamp("create_time");
        supplier.updateTime = rs.getTimestamp("update_time");
        return supplier;
    }
}