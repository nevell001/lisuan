package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发票文件路径的「触发条件」门禁（TD-001）。
 *
 * <p>背景：{@code POST /api/invoices/{id}/print} 会把请求体里的 {@code pdfPath}/{@code imagePath}
 * 原样落库（{@code invoices.pdf_path} / {@code image_path}），而**全仓库没有任何地方把它们当文件路径读取**。
 * 因此现在不是漏洞，只是一段"写进去的任意字符串"；此时加白名单属于为不存在的能力做防御，
 * 而且合法路径范围（导出目录？用户自选目录？外部电子发票服务的共享盘？）取决于尚未确定的
 * 发票预览/下载设计，现在定下来的白名单大概率是错的。</p>
 *
 * <p>本门禁把"将来别忘了"变成 CI 红灯：只要有人把这两个字段当文件系统路径**解引用**
 * （读或写：{@code new File}、{@code Files.*}、{@code Path.of}、{@code FileInputStream}…），
 * 测试立即失败，并要求**同一次提交内**实现路径校验后再把这个站点登记进
 * {@link #VALIDATED_READ_SITES}（每条都要注明校验方式，与仓库其它白名单的做法一致）。</p>
 *
 * <p><b>已知边界</b>：解引用检测按"使用点同一行或前后三行"匹配，覆盖
 * {@code Path.of(invoice.pdfPath)} 这类直接写法；若有人先把路径存进局部变量、十几行后再打开，
 * 本门禁抓不到——这种间接用法要靠 code review。要收紧的话，把匹配范围从"±3 行"改成
 * "所在方法体内"即可，但会引入误报（迁移/DDL 方法里同时有列名与文件操作），需一并处理白名单。</p>
 */
@DisplayName("发票路径触发条件门禁")
class InvoicePathGuardPolicyTest {

    /**
     * 已实现路径校验、允许解引用发票路径的站点：{@code 文件名#方法名 — 校验方式}。
     *
     * <p>现在是空的——不是遗漏，而是因为还没有任何读取方。实现发票预览/下载时，
     * 请先做校验（相对路径 + 固定根目录 + 扩展名白名单 + 规范化前缀校验 + 读取前二次校验，
     * 参考 {@code ExportUtil} / {@code BackupService}），再把站点登记到这里并写明理由。</p>
     */
    private static final List<String> VALIDATED_READ_SITES = List.of(
        // 例： "InvoicePreviewService.java#openPdf — safeInvoicePath() 校验相对路径 + 固定根前缀 + .pdf/.png/.jpg"
    );

    /** 把字符串当文件系统路径用的 API（读与写都算）。 */
    private static final Pattern DEREFERENCE = Pattern.compile(
        "new\\s+File\\s*\\(|new\\s+FileInputStream\\s*\\(|new\\s+FileOutputStream\\s*\\(|"
            + "Files\\.(read|write|newInputStream|newOutputStream|copy|lines|walk|delete|move|exists)|"
            + "Paths?\\.get\\s*\\(|Path\\.of\\s*\\(|\\.toURI\\s*\\(\\s*\\)|getResourceAsStream");

    /** 发票路径字段/列名（带点或下划线，避免误伤 {@code LogoPrinter} 里同名的局部参数）。 */
    private static final Pattern INVOICE_PATH = Pattern.compile(
        "\\.(pdfPath|imagePath)\\b|\\b(pdf_path|image_path|PDF_PATH|IMAGE_PATH)\\b");

    /**
     * 必须分别在这两个文件里找到使用点，否则说明字段/列名改了、本门禁的识别规则已失效（空转的假绿灯）。
     * 只用"总数量 ≥ N"是不够的：DAO 里的列名就有好几处，识别规则少一半也照样满足（变异验证踩过）。
     */
    private static final List<String> MUST_APPEAR_IN = List.of(
        "InvoiceApiController.java", "InvoiceDAORefactored.java");

    @Test
    @DisplayName("发票路径字段不得在未登记的情况下被当成文件路径解引用")
    void invoicePathsAreNeverDereferencedAsFiles() throws Exception {
        List<String> violations = new ArrayList<>();
        List<String> usages = new ArrayList<>();

        for (Path file : javaSources()) {
            List<String> lines = Files.readAllLines(file);
            String text = String.join("\n", lines);
            for (int i = 0; i < lines.size(); i++) {
                if (!INVOICE_PATH.matcher(lines.get(i)).find()) {
                    continue;
                }
                usages.add(file + ":" + (i + 1));
                // 同一行或前后三行内出现解引用 → 视为把这个字段当路径用了
                int from = Math.max(0, i - 3);
                int to = Math.min(lines.size() - 1, i + 3);
                Matcher deref = DEREFERENCE.matcher(String.join("\n", lines.subList(from, to + 1)));
                if (deref.find()) {
                    String site = file.getFileName() + "#" + enclosingMethod(text, file, i + 1);
                    if (VALIDATED_READ_SITES.stream().noneMatch(entry -> entry.startsWith(site))) {
                        violations.add(file + ":" + (i + 1) + "  →  " + lines.get(i).trim()
                            + "\n        附近出现路径解引用: " + deref.group());
                    }
                }
            }
        }

        assertTrue(violations.isEmpty(),
            "有代码把发票路径（invoices.pdf_path / image_path）当文件系统路径用了，但没有同批实现路径校验，"
                + "也没有登记到 InvoicePathGuardPolicyTest.VALIDATED_READ_SITES（TD-001）。\n"
                + "合法路径范围取决于产品设计，必须与「发票预览/下载」需求同一次实现：\n"
                + "  · 只接受相对路径 + 固定根目录，拒绝绝对路径与 `..`\n"
                + "  · 落库前按规范化后的路径做前缀校验（参考 ExportUtil / BackupService）\n"
                + "  · 读取时二次校验（TOCTOU），并限制扩展名 .pdf/.png/.jpg\n"
                + "  · 补一条\"路径穿越被拒\"的回归测试\n"
                + "完成后再把站点登记进 VALIDATED_READ_SITES 并注明校验方式：\n  "
                + String.join("\n  ", violations));

        for (String required : MUST_APPEAR_IN) {
            assertTrue(usages.stream().anyMatch(usage -> usage.contains(required)),
                "在 " + required + " 里没找到发票路径使用点：字段名/列名可能改了，"
                    + "请同步更新本门禁的识别规则，否则它会变成空转的假绿灯。实际找到：" + usages);
        }
    }

    @Test
    @DisplayName("写入点必须保留“故意不校验”的说明，指向 TD-001")
    void writeSiteKeepsTheDeliberateChoiceDocumented() throws IOException {
        String controller = Files.readString(
            Path.of("src/main/java/com/cashier/api/controller/InvoiceApiController.java"));
        assertTrue(controller.contains("TD-001"),
            "InvoiceApiController 里记录发票路径的地方要保留一句说明：当前**故意不校验**、原因见 TD-001、"
                + "以及将来实现预览/下载时必须同批做校验——否则后人会把\"没校验\"误读成漏写");
        assertTrue(controller.contains("pdfPath") && controller.contains("imagePath"),
            "写入点应当仍在处理 pdfPath/imagePath（若字段改名，请同步本门禁）");
    }

    /** 粗略定位某一行的所属方法名（用于白名单键 `文件名#方法名`）。 */
    private static String enclosingMethod(String text, Path file, int line) {
        List<String> lines = List.of(text.split("\n", -1));
        Pattern method = Pattern.compile(
            "^\\s{4}(?:public|private|protected)\\s+(?:static\\s+)?[\\w<>,\\[\\]\\.\\s]+\\s+(\\w+)\\s*\\(");
        String current = "(top-level)";
        for (int i = 0; i < line && i < lines.size(); i++) {
            Matcher matcher = method.matcher(lines.get(i));
            if (matcher.find()) {
                current = matcher.group(1);
            }
        }
        return current;
    }

    private static List<Path> javaSources() throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
            files.addAll(walk.filter(p -> p.toString().endsWith(".java")).toList());
        }
        assertTrue(!files.isEmpty(), "未找到 Java 源码，路径不对？");
        return files;
    }
}
