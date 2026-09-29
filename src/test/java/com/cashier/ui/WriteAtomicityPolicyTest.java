package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 写操作原子性门禁（2026-09 审计）。
 *
 * <p>本仓库的 DAO 里有两套写法：{@code xxx(...)} 自带 autocommit 连接，{@code xxxWithConnection(conn, ...)}
 * 参与调用方的事务。多表/多行写入时**必须**走后者并包在 {@code executeBooleanTransaction} 里，
 * 否则中途失败会留下"改了一半"的数据。审计发现并修掉了四处，这里逐条钉住：</p>
 * <ol>
 *   <li>盘点单保存（表头 + 明细重建）：原来删完旧明细再逐条插，各自 autocommit，
 *       失败即永久丢失明细；</li>
 *   <li>手工开票（表头 + 明细）：原来 insert 用自带连接，明细失败留下一张"有金额无明细"的孤儿发票；</li>
 *   <li>退款还原库存：原来丢弃 UPDATE 的返回值，库存没还回去也报"退款成功"；</li>
 *   <li>建库/迁移失败被 catch 吞掉：应用照常启动，之后每个页面都报"表不存在"。</li>
 * </ol>
 */
@DisplayName("写操作原子性门禁")
class WriteAtomicityPolicyTest {

    private static final Path SRC = Path.of("src/main/java/com/cashier");

    @Test
    @DisplayName("盘点单保存：表头与明细必须同一事务，且拒绝空明细")
    void inventoryCheckSaveIsAtomic() throws Exception {
        String source = Files.readString(SRC.resolve("controller/InventoryCheckController.java"));
        String code = withoutComments(source);
        String body = methodBody(source, "private void handleSaveCheck(");

        assertTrue(body.contains("executeBooleanTransaction("),
            "盘点单保存必须包在 executeBooleanTransaction 里：编辑路径会先删掉全部旧明细，"
                + "各自 autocommit 时中途失败会留下已提交的 DELETE + 半截明细");
        assertTrue(body.contains("updateWithConnection(conn, newCheck)"),
            "编辑盘点单必须用 inventoryCheckDAO.updateWithConnection");
        assertTrue(body.contains("deleteByCheckIdWithConnection(conn, newCheck.id)"),
            "删除旧明细必须用 deleteByCheckIdWithConnection，才能被同一事务回滚");
        assertTrue(body.contains("insertWithConnection(conn,"),
            "明细必须用 insertWithConnection 参与同一事务");
        assertTrue(body.contains("items.isEmpty()"),
            "必须拒绝空明细：明细加载失败时列表为空，空列表提交会把盘点单明细整批清空");

        assertFalse(code.contains("inventoryCheckItemDAO.insert("),
            "handleSaveCheck 不得再用自带连接的 inventoryCheckItemDAO.insert(...)（事务外提交）");
        assertFalse(code.contains("inventoryCheckItemDAO.deleteByCheckId("),
            "handleSaveCheck 不得再用自带连接的 deleteByCheckId(...)（事务外提交）");
        assertFalse(code.contains("inventoryCheckDAO.update(newCheck)"),
            "handleSaveCheck 不得再用自带连接的 update(newCheck)");
        assertFalse(code.contains("inventoryCheckDAO.insert(newCheck)"),
            "handleSaveCheck 不得再用自带连接的 insert(newCheck)");
    }

    @Test
    @DisplayName("手工开票：表头与明细必须同一事务")
    void manualInvoiceInsertIsAtomic() throws Exception {
        String source = Files.readString(SRC.resolve("service/InvoiceService.java"));
        String body = methodBody(source, "public static Invoice createManualInvoice(");

        assertTrue(body.contains("executeBooleanTransaction("),
            "手工开票必须包在事务里：insert() 用自己的 autocommit 连接，表头先提交、"
                + "明细后插，明细失败就留下有金额无明细的孤儿发票");
        assertTrue(body.contains("insertWithConnection(conn, invoice)"),
            "手工开票必须走 insertWithConnection 参与调用方的事务");
        assertFalse(withoutComments(body).contains("getInvoiceDAO().insert(invoice)"),
            "手工开票不得再用自带连接的 insert(invoice)（表头会在明细之前提交）");
    }

    @Test
    @DisplayName("退款还原库存：必须消费 UPDATE 的返回值")
    void refundRestoreConsumesUpdateResult() throws Exception {
        String source = Files.readString(SRC.resolve("api/controller/TransactionApiController.java"));
        String body = methodBody(source, "private static void restoreInventoryForRefund(");

        assertTrue(body.contains("if (!productDAO.updateQuantityWithConnection("),
            "退款还原库存必须判断返回值：丢弃它会让库存根本没还回去也照样报'退款成功'");
        assertTrue(body.contains("logger.warn("),
            "商品行不存在/明细缺商品ID时必须留 WARN 日志（钱照退，但要可追溯）");
    }

    @Test
    @DisplayName("相对增减库存必须递增 version，避免被并发结账覆盖")
    void stockQuantityUpdateBumpsVersion() throws Exception {
        String source = Files.readString(SRC.resolve("dao/ProductDAORefactored.java"));
        String body = methodBody(source, "public boolean updateQuantityWithConnection(");

        assertTrue(body.contains("version = version + 1"),
            "updateQuantityWithConnection 必须同时 version = version + 1："
                + "结账扣库存是 'quantity 绝对值 + WHERE version=?'，这里不动 version 时，"
                + "并发结账会拿旧 version 命中并把刚还回的库存覆盖掉");
    }

    @Test
    @DisplayName("建库/迁移失败不得被吞掉（否则启动看似成功，页面全是 SQL 报错）")
    void schemaInitializationFailureIsNotSwallowed() throws Exception {
        String source = Files.readString(SRC.resolve("util/DatabaseManager.java"));
        String signature = "private static void initializeDatabase(";
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到 initializeDatabase 方法");
        String declaration = source.substring(start, source.indexOf('{', start));
        assertTrue(declaration.contains("throws SQLException"),
            "initializeDatabase 必须声明 throws SQLException，才能把建表/迁移失败交给静态块统一处理");

        String body = methodBody(source, signature);
        assertTrue(body.contains("throw e;"),
            "initializeDatabase 的 catch 必须把 SQLException 抛出去，"
                + "由静态块统一转换成'数据库初始化失败 + 排查指引'");
    }

    /** 取方法体（按花括号配对）。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法签名: " + signature);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new AssertionError("方法体不闭合: " + signature);
    }

    /** 去掉注释（保留字符串字面量），避免注释里的示例文本把门禁写红。 */
    private static String withoutComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        boolean inString = false;
        boolean inChar = false;
        boolean inLine = false;
        boolean inBlock = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (inLine) {
                inLine = c != '\n';
                out.append(c == '\n' ? c : ' ');
            } else if (inBlock) {
                if (c == '*' && next == '/') {
                    inBlock = false;
                    out.append("  ");
                    i++;
                } else {
                    out.append(c == '\n' ? c : ' ');
                }
            } else if (inString || inChar) {
                out.append(c);
                if (c == '\\' && next != '\0') {
                    out.append(next);
                    i++;
                } else if (inString && c == '"') {
                    inString = false;
                } else if (inChar && c == '\'') {
                    inChar = false;
                }
            } else if (c == '/' && next == '/') {
                inLine = true;
                out.append("  ");
                i++;
            } else if (c == '/' && next == '*') {
                inBlock = true;
                out.append("  ");
                i++;
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '\'') {
                    inChar = true;
                }
                out.append(c);
            }
        }
        return out.toString();
    }
}
