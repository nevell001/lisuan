package com.cashier.dao;

import com.cashier.model.Member;
import com.cashier.model.PageResult;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class MemberDAORefactored extends BaseDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(MemberDAORefactored.class);

    private static final String SELECT_COLUMNS =
        "id, member_code, phone, name, points, level, discount, balance, birthday, version ";

    private static final RowMapper<Member> MEMBER_MAPPER = new RowMapper<Member>() {
        @Override
        public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
            Member member = new Member();
            member.id = rs.getInt("id");
            member.memberCode = rs.getString("member_code");
            member.phone = rs.getString("phone");
            member.name = rs.getString("name");
            member.points = rs.getBigDecimal("points");
            member.level = rs.getString("level");
            member.discount = rs.getBigDecimal("discount");
            member.discountRate = member.discount;
            member.balance = rs.getBigDecimal("balance");
            member.birthday = rs.getString("birthday");
            try {
                member.version = rs.getInt("version");
            } catch (SQLException e) {
                member.version = 0;
            }
            return member;
        }
    };

    public Member findById(int id) throws SQLException {
        try (Connection conn = getConnection()) {
            return findByIdWithConnection(conn, id);
        }
    }

    public Member findByIdWithConnection(Connection conn, int id) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT " + SELECT_COLUMNS + " FROM members WHERE id = ?")) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? MEMBER_MAPPER.mapRow(rs, 0) : null;
            }
        }
    }

    public Member findByPhone(String phone) throws SQLException {
        try (Connection conn = getConnection()) {
            return findByPhoneWithConnection(conn, phone);
        }
    }

    public Member findByPhoneWithConnection(Connection conn, String phone) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT " + SELECT_COLUMNS + " FROM members WHERE phone = ?")) {
            pstmt.setString(1, phone);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? MEMBER_MAPPER.mapRow(rs, 0) : null;
            }
        }
    }

    public List<Member> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM members ORDER BY name", MEMBER_MAPPER);
    }

    public PageResult<Member> findAll(int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
        long total = count();
        int offset = (pageNum - 1) * pageSize;
        List<Member> members = queryList("SELECT " + SELECT_COLUMNS +
            " FROM members ORDER BY name LIMIT ? OFFSET ?", MEMBER_MAPPER, pageSize, offset);
        return new PageResult<>(members, pageNum, pageSize, total);
    }

    public long count() throws SQLException {
        return queryLong("SELECT COUNT(*) FROM members");
    }

    public Map<String, Object> getMemberSummary() throws SQLException {
        String sql = "SELECT COUNT(*) AS total_count, COALESCE(SUM(balance), 0) AS total_balance, " +
            "COALESCE(SUM(points), 0) AS total_points FROM members";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("totalCount", rs.getLong("total_count"));
                summary.put("totalBalance", rs.getBigDecimal("total_balance"));
                summary.put("totalPoints", rs.getBigDecimal("total_points"));
                return summary;
            }
        }
        Map<String, Object> emptySummary = new HashMap<>();
        emptySummary.put("totalCount", 0L);
        emptySummary.put("totalBalance", BigDecimal.ZERO);
        emptySummary.put("totalPoints", BigDecimal.ZERO);
        return emptySummary;
    }

    public Map<String, Integer> countByLevel() throws SQLException {
        String sql = "SELECT level, COUNT(*) AS level_count FROM members GROUP BY level";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            Map<String, Integer> levelStats = new HashMap<>();
            while (rs.next()) {
                levelStats.put(rs.getString("level"), rs.getInt("level_count"));
            }
            return levelStats;
        }
    }

    public boolean insert(Member member) throws SQLException {
        if (member.memberCode == null || member.memberCode.trim().isEmpty()) {
            member.memberCode = generateMemberCode();
        }
        // MAX+1 取号在多进程并发下可能撞 member_code 唯一键；
        // 确认是该编号冲突时重新取号重试（最多 3 次），避免并发创建会员直接失败。
        final int maxAttempts = 3;
        for (int attempt = 1; ; attempt++) {
            try {
                if (member.id > 0) {
                    return executeUpdate(
                        "INSERT INTO members (id, member_code, phone, name, points, level, discount, balance, birthday, version) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        member.id, member.memberCode, member.phone, member.name, member.points,
                        member.level, member.discount, member.balance, member.birthday, 0) > 0;
                }
                long id = executeInsertReturnId(
                    "INSERT INTO members (member_code, phone, name, points, level, discount, balance, birthday) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    member.memberCode, member.phone, member.name, member.points,
                    member.level, member.discount, member.balance, member.birthday);
                member.id = (int) id;
                return id > 0;
            } catch (SQLException e) {
                // 仅当确实是 member_code 撞号时重试；其他异常（电话重复、连接异常等）原样抛出
                if (attempt >= maxAttempts || !isMemberCodeTaken(member.memberCode)) {
                    throw e;
                }
                logger.warn("会员编号撞号，重新取号重试: attempt={}, code={}", attempt, member.memberCode);
                member.memberCode = generateMemberCode();
            }
        }
    }

    private boolean isMemberCodeTaken(String memberCode) throws SQLException {
        return queryInt("SELECT COUNT(*) FROM members WHERE member_code = ?", memberCode) > 0;
    }

    private String generateMemberCode() throws SQLException {
        String dateStr = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.COMPACT_DATE);
        String sql = "SELECT member_code FROM members WHERE member_code LIKE ? ORDER BY member_code DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "MEM" + dateStr + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                int sequence = 1;
                if (rs.next()) {
                    String lastCode = rs.getString("member_code");
                    try {
                        String lastSeq = lastCode.substring(lastCode.length() - 4);
                        sequence = Integer.parseInt(lastSeq) + 1;
                    } catch (Exception e) {
                        // 解析失败使用默认值
                    }
                }
                return String.format("MEM%s%04d", dateStr, sequence);
            }
        }
    }

    public boolean update(Member member) throws SQLException {
        try (Connection conn = getConnection()) {
            return updateWithConnection(conn, member);
        }
    }

    public boolean updateWithConnection(Connection conn, Member member) throws SQLException {
        String sql = "UPDATE members SET member_code = ?, phone = ?, name = ?, points = ?, level = ?, discount = ?, " +
            "balance = ?, birthday = ?, version = version + 1 WHERE id = ? AND version = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, member.memberCode);
            pstmt.setString(2, member.phone);
            pstmt.setString(3, member.name);
            pstmt.setBigDecimal(4, member.points);
            pstmt.setString(5, member.level);
            pstmt.setBigDecimal(6, member.discount);
            pstmt.setBigDecimal(7, member.balance);
            pstmt.setString(8, member.birthday);
            pstmt.setInt(9, member.id);
            pstmt.setInt(10, member.version);
            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                logger.warn("会员更新乐观锁冲突: id={}, 期望version={}", member.id, member.version);
                throw new SQLException("会员数据已被其他操作修改，请重试 (id=" + member.id + ")");
            }
            member.version++;
            return true;
        }
    }

    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM members WHERE id = ?", id) > 0;
    }

    public boolean deleteByPhone(String phone) throws SQLException {
        return executeUpdate("DELETE FROM members WHERE phone = ?", phone) > 0;
    }

    public boolean updatePoints(int id, double delta) throws SQLException {
        return executeUpdate("UPDATE members SET points = points + ? WHERE id = ?",
            BigDecimal.valueOf(delta), id) > 0;
    }

    public boolean updatePointsWithConnection(Connection conn, int id, double delta) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE members SET points = points + ? WHERE id = ?")) {
            pstmt.setBigDecimal(1, BigDecimal.valueOf(delta));
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updatePointsByPhone(String phone, double delta) throws SQLException {
        return executeUpdate("UPDATE members SET points = points + ? WHERE phone = ?",
            BigDecimal.valueOf(delta), phone) > 0;
    }

    public boolean updateBalance(int id, double delta) throws SQLException {
        return executeUpdate("UPDATE members SET balance = balance + ? WHERE id = ?",
            BigDecimal.valueOf(delta), id) > 0;
    }

    public boolean updateBalanceByPhone(String phone, double delta) throws SQLException {
        return executeUpdate("UPDATE members SET balance = balance + ? WHERE phone = ?",
            BigDecimal.valueOf(delta), phone) > 0;
    }

    public List<Member> search(String keyword) throws SQLException {
        String pattern = "%" + keyword + "%";
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM members WHERE name LIKE ? OR phone LIKE ? ORDER BY name", MEMBER_MAPPER, pattern, pattern);
    }

    public PageResult<Member> search(String keyword, int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
        String pattern = "%" + keyword + "%";
        long total = countByKeyword(pattern);
        int offset = (pageNum - 1) * pageSize;
        List<Member> members = queryList("SELECT " + SELECT_COLUMNS +
            " FROM members WHERE name LIKE ? OR phone LIKE ? ORDER BY name LIMIT ? OFFSET ?",
            MEMBER_MAPPER, pattern, pattern, pageSize, offset);
        return new PageResult<>(members, pageNum, pageSize, total);
    }

    private long countByKeyword(String pattern) throws SQLException {
        return queryLong("SELECT COUNT(*) FROM members WHERE name LIKE ? OR phone LIKE ?", pattern, pattern);
    }

    public List<Member> findByLevel(String level) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM members WHERE level = ? ORDER BY points DESC", MEMBER_MAPPER, level);
    }

    public void batchInsert(List<Member> members) throws SQLException {
        List<Object[]> params = new ArrayList<>(members.size());
        for (Member member : members) {
            params.add(new Object[]{
                member.phone, member.name, member.points, member.level,
                member.discount, member.balance, member.birthday});
        }
        batchUpdate(
            "INSERT INTO members (phone, name, points, level, discount, balance, birthday) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", params);
    }
}
