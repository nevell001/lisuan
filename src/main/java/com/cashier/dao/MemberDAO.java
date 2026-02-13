package com.cashier.dao;

import com.cashier.model.Member;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 会员数据访问对象
 * 负责会员相关的数据库操作
 */
public class MemberDAO {

    /**
     * 根据ID查找会员
     */
    public static Member findById(int id) throws SQLException {
            String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday FROM members WHERE id = ?";
    
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
    
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
    
                if (rs.next()) {
                    return mapRowToMember(rs);
                }
            }
    
            return null;
        }

    /**
     * 根据手机号查找会员
     */
    public static Member findByPhone(String phone) throws SQLException {
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday FROM members WHERE phone = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, phone);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToMember(rs);
            }
        }
        return null;
    }

    /**
     * 查询所有会员
     */
    public static List<Member> findAll() throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday FROM members ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                members.add(mapRowToMember(rs));
            }
        }
        return members;
    }

    /**
     * 插入新会员
     * 如果会员ID大于0，则使用指定的ID；否则由数据库自动生成ID
     */
    public static boolean insert(Member member) throws SQLException {
        // 自动生成会员编号
        if (member.memberCode == null || member.memberCode.trim().isEmpty()) {
            member.memberCode = generateMemberCode();
        }

        String sql;
        boolean useProvidedId = member.id > 0;

        if (useProvidedId) {
            // 使用用户提供的ID
            sql = "INSERT INTO members (id, member_code, phone, name, points, level, discount, balance, birthday) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            // 由数据库自动生成ID
            sql = "INSERT INTO members (member_code, phone, name, points, level, discount, balance, birthday) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (useProvidedId) {
                pstmt.setInt(paramIndex++, member.id);
            }

            pstmt.setString(paramIndex++, member.memberCode);
            pstmt.setString(paramIndex++, member.phone);
            pstmt.setString(paramIndex++, member.name);
            pstmt.setDouble(paramIndex++, member.points);
            pstmt.setString(paramIndex++, member.level);
            pstmt.setDouble(paramIndex++, member.discount);
            pstmt.setDouble(paramIndex++, member.balance);
            pstmt.setString(paramIndex++, member.birthday);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0 && !useProvidedId) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        member.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 生成会员编号
     * @return 会员编号
     */
    private static String generateMemberCode() throws SQLException {
        // 格式: MEM + yyyyMMdd + 4位递增序号
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new java.util.Date());
        
        String sql = "SELECT member_code FROM members WHERE member_code LIKE ? ORDER BY member_code DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "MEM" + dateStr + "%");
            ResultSet rs = pstmt.executeQuery();
            
            int sequence = 1;
            if (rs.next()) {
                String lastCode = rs.getString("member_code");
                // 提取序号部分（最后4位）
                try {
                    String lastSeq = lastCode.substring(lastCode.length() - 4);
                    sequence = Integer.parseInt(lastSeq) + 1;
                } catch (Exception e) {
                    // 如果解析失败，使用默认值
                }
            }
            
            // 格式化为4位数字，前面补0
            return String.format("MEM%s%04d", dateStr, sequence);
        }
    }

    /**
     * 更新会员
     */
    public static boolean update(Member member) throws SQLException {
        String sql = "UPDATE members SET member_code = ?, phone = ?, name = ?, points = ?, level = ?, discount = ?, balance = ?, birthday = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, member.memberCode);
            pstmt.setString(2, member.phone);
            pstmt.setString(3, member.name);
            pstmt.setDouble(4, member.points);
            pstmt.setString(5, member.level);
            pstmt.setDouble(6, member.discount);
            pstmt.setDouble(7, member.balance);
            pstmt.setString(8, member.birthday);
            pstmt.setInt(9, member.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除会员
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM members WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据手机号删除会员（兼容旧代码）
     */
    public static boolean deleteByPhone(String phone) throws SQLException {
        String sql = "DELETE FROM members WHERE phone = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, phone);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新会员积分
     */
    public static boolean updatePoints(int id, double delta) throws SQLException {
        String sql = "UPDATE members SET points = points + ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, delta);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据手机号更新会员积分（兼容旧代码）
     */
    public static boolean updatePointsByPhone(String phone, double delta) throws SQLException {
        String sql = "UPDATE members SET points = points + ? WHERE phone = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, delta);
            pstmt.setString(2, phone);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新会员余额
     */
    public static boolean updateBalance(int id, double delta) throws SQLException {
        String sql = "UPDATE members SET balance = balance + ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, delta);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据手机号更新会员余额（兼容旧代码）
     */
    public static boolean updateBalanceByPhone(String phone, double delta) throws SQLException {
        String sql = "UPDATE members SET balance = balance + ? WHERE phone = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, delta);
            pstmt.setString(2, phone);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 搜索会员（按姓名或手机号）
     */
    public static List<Member> search(String keyword) throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, phone, name, points, level, discount, balance, birthday FROM members " +
                     "WHERE name LIKE ? OR phone LIKE ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(mapRowToMember(rs));
            }
        }
        return members;
    }

    /**
     * 根据等级查询会员
     */
    public static List<Member> findByLevel(String level) throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, phone, name, points, level, discount, balance, birthday FROM members " +
                     "WHERE level = ? ORDER BY points DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, level);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(mapRowToMember(rs));
            }
        }
        return members;
    }

    /**
     * 批量插入会员
     */
    public static void batchInsert(List<Member> members) throws SQLException {
        String sql = "INSERT INTO members (phone, name, points, level, discount, balance, birthday) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Member member : members) {
                pstmt.setString(1, member.phone);
                pstmt.setString(2, member.name);
                pstmt.setDouble(3, member.points);
                pstmt.setString(4, member.level);
                pstmt.setDouble(5, member.discount);
                pstmt.setDouble(6, member.balance);
                pstmt.setString(7, member.birthday);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Member 对象
     */
    private static Member mapRowToMember(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.id = rs.getInt("id");
        member.memberCode = rs.getString("member_code");
        member.phone = rs.getString("phone");
        member.name = rs.getString("name");
        member.points = rs.getDouble("points");
        member.level = rs.getString("level");
        member.discount = rs.getDouble("discount");
        member.discountRate = member.discount;
        member.balance = rs.getDouble("balance");
        member.birthday = rs.getString("birthday");
        return member;
    }
}
