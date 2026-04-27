package com.cashier.dao;

import com.cashier.model.Specification;
import com.cashier.util.DatabaseManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品规格类型数据访问对象
 */
public class SpecificationDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(SpecificationDAO.class);

    /**
     * 插入规格类型
     */
    public static int insert(Specification specification) throws SQLException {
        String sql = "INSERT INTO specifications (name, code, type, description, sort_order, enabled, create_time, update_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, specification.name);
            pstmt.setString(2, specification.code);
            pstmt.setString(3, specification.type);
            pstmt.setString(4, specification.description);
            pstmt.setInt(5, specification.sortOrder);
            pstmt.setBoolean(6, specification.enabled);
            pstmt.setTimestamp(7, new Timestamp(specification.createTime.getTime()));
            pstmt.setTimestamp(8, new Timestamp(specification.updateTime.getTime()));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            
            return 0;
        }
    }

    /**
     * 更新规格类型
     */
    public static boolean update(Specification specification) throws SQLException {
        String sql = "UPDATE specifications SET name = ?, code = ?, type = ?, description = ?, " +
                     "sort_order = ?, enabled = ?, update_time = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            specification.updateTime = new java.util.Date();
            
            pstmt.setString(1, specification.name);
            pstmt.setString(2, specification.code);
            pstmt.setString(3, specification.type);
            pstmt.setString(4, specification.description);
            pstmt.setInt(5, specification.sortOrder);
            pstmt.setBoolean(6, specification.enabled);
            pstmt.setTimestamp(7, new Timestamp(specification.updateTime.getTime()));
            pstmt.setInt(8, specification.id);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除规格类型
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM specifications WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据ID查找规格类型
     */
    public static Specification findById(int id) throws SQLException {
        String sql = "SELECT id, name, code, type, description, sort_order, enabled, create_time, update_time FROM specifications WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSpecification(rs);
                }
            }
        }
        
        return null;
    }

    /**
     * 查找所有规格类型
     */
    public static List<Specification> findAll() throws SQLException {
        String sql = "SELECT id, name, code, type, description, sort_order, enabled, create_time, update_time FROM specifications ORDER BY sort_order ASC, id ASC";
        
        List<Specification> specifications = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                specifications.add(mapRowToSpecification(rs));
            }
        }
        
        return specifications;
    }

    /**
     * 根据类型查找规格类型
     */
    public static List<Specification> findByType(String type) throws SQLException {
        String sql = "SELECT id, name, code, type, description, sort_order, enabled, create_time, update_time FROM specifications WHERE type = ? ORDER BY sort_order ASC, id ASC";
        
        List<Specification> specifications = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, type);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    specifications.add(mapRowToSpecification(rs));
                }
            }
        }
        
        return specifications;
    }

    /**
     * 查找启用的规格类型
     */
    public static List<Specification> findEnabled() throws SQLException {
        String sql = "SELECT id, name, code, type, description, sort_order, enabled, create_time, update_time FROM specifications WHERE enabled = true ORDER BY sort_order ASC, id ASC";
        
        List<Specification> specifications = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                specifications.add(mapRowToSpecification(rs));
            }
        }
        
        return specifications;
    }

    /**
     * 将ResultSet映射为Specification对象
     */
    private static Specification mapRowToSpecification(ResultSet rs) throws SQLException {
        Specification specification = new Specification();
        specification.id = rs.getInt("id");
        specification.name = rs.getString("name");
        specification.code = rs.getString("code");
        specification.type = rs.getString("type");
        specification.description = rs.getString("description");
        specification.sortOrder = rs.getInt("sort_order");
        specification.enabled = rs.getBoolean("enabled");
        specification.createTime = rs.getTimestamp("create_time");
        specification.updateTime = rs.getTimestamp("update_time");
        return specification;
    }
}