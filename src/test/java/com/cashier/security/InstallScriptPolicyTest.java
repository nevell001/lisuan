package com.cashier.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 安装/运维脚本的安全与"失败可见"门禁（TD-012）。
 *
 * <p>这些脚本只在部署机上跑，CI 平时看不到；但它们决定了"装完到底成没成"和"会不会把占位口令
 * 写进真实数据库"。本类把三条不变量钉住：</p>
 * <ol>
 *   <li>{@code install.sh} 的 SQL 导入失败必须报错并退出非零（此前 {@code || true} 把失败吃掉，
 *       脚本照样打印 {@code [Done] Database initialization completed}）；</li>
 *   <li>{@code docker-init.sh} 必须先读工作目录的 {@code .env}，且空/占位口令一律硬失败
 *       （此前只警告，用户答 y 就会把 {@code YOUR_CASHIER_PASSWORD_HERE} 真的 ALTER USER 进 MySQL）；</li>
 *   <li>容器名等部署变量必须与 {@code install.sh} / compose 用同一个来源，不能各自硬编码。</li>
 * </ol>
 */
@DisplayName("安装脚本安全门禁")
class InstallScriptPolicyTest {

    private static final Path INSTALL_SH = Path.of("install.sh");
    private static final Path DOCKER_INIT = Path.of("docker/docker-init.sh");
    private static final Path START_MYSQL = Path.of("docker/start-mysql.sh");
    private static final Path ENV_EXAMPLE = Path.of(".env.example");
    private static final Path CREDENTIALS = Path.of("docs/CREDENTIALS_CHECKLIST.md");

    @Test
    @DisplayName("install.sh 的数据库导入失败必须报错退出，不得再静默成功")
    void installFailsLoudlyWhenDatabaseImportFails() throws Exception {
        String script = Files.readString(INSTALL_SH);

        assertFalse(script.contains("00-init-complete.sql 2>/dev/null || true"),
            "SQL 导入不得再用 '2>/dev/null || true' 吞掉失败：数据库是空的，脚本却会打印初始化完成");

        // 三条路径（Docker / 本地 / 远程）的建库与导入共 7 处失败分支，都要走同一个处理函数
        assertEquals(7, count(script, "fail_db_import \""),
            "建库/导入的每个失败分支都必须调用 fail_db_import（Docker 3 + 本地 2 + 远程 2）");
        assertTrue(script.contains("fail_db_import() {"), "应存在统一的失败处理函数");
        assertTrue(script.contains("exit 1"), "失败处理必须退出非零");

        // 导入失败时要能看到 mysql 的真实错误，而不是被丢进 /dev/null
        assertEquals(3, count(script, "2>\"$DB_IMPORT_ERR\""),
            "三条路径的导入都要把 stderr 收进临时文件，失败时打印出来");

        // 成功提示只应出现在成功路径上：三条路径各一次，且都在 fail_db_import 定义之后
        // （只看 echo 那一行——注释里也会提到这句文案，别被注释骗了）
        String doneEcho = "echo \"[Done] Database initialization completed";
        assertEquals(3, count(script, doneEcho), "三条路径各应有一次成功提示");
        assertTrue(script.indexOf(doneEcho) > script.indexOf("fail_db_import() {"),
            "成功提示应定义在失败处理之后，且失败分支不得复用该文案");
    }

    @Test
    @DisplayName("docker-init.sh 必须先读 .env，且空/占位口令硬失败")
    void dockerInitReadsEnvAndRejectsPlaceholderPasswords() throws Exception {
        String script = Files.readString(DOCKER_INIT);

        assertTrue(script.contains(". ./.env"),
            "docker-init.sh 必须先 source 工作目录的 .env：只做了 cp .env.example .env 的机器"
                + "否则会退化成脚本里的占位常量");
        assertTrue(script.contains("is_placeholder_password"),
            "需要显式的占位口令判定（空值、YOUR_*_HERE、changeme 等）");
        assertFalse(script.contains("是否继续？"),
            "占位口令不得再提供「确认后继续」：那会把占位口令真的写进 MySQL 与 .env");
        assertTrue(script.contains("已停止初始化"), "占位口令时必须明确停止并说明原因");

        // 守卫必须排在第一条真正的 ALTER USER 语句之前，否则等于没拦
        // （从守卫处往后找：注释里也会提到 ALTER USER，别被注释骗了）
        // 守卫必须是真正的可执行判断（不能被 if false && 短路掉，那种改法文本还在但等于没拦）
        String guardLine = "if is_placeholder_password \"$MYSQL_ROOT_PASSWORD\" "
            + "|| is_placeholder_password \"$CASHIER_DB_PASSWORD\"; then";
        int guard = script.indexOf(guardLine);
        assertTrue(guard > 0, "占位口令守卫必须是可执行判断，实际未找到: " + guardLine);
        int alterUser = script.indexOf("ALTER USER 'root'@", guard);
        assertTrue(alterUser > guard,
            "占位口令检查必须出现在第一条 ALTER USER 之前");
    }

    @Test
    @DisplayName("容器名必须与 install.sh / compose 同源，不得硬编码")
    void containerNameComesFromOneSource() throws Exception {
        String startMysql = Files.readString(START_MYSQL);
        String install = Files.readString(INSTALL_SH);

        assertFalse(startMysql.contains("grep -q \"lisuan-mysql\""),
            "docker/start-mysql.sh 不得硬编码容器名：自定义 MYSQL_CONTAINER_NAME 时会误报「启动失败」");
        assertTrue(startMysql.contains("MYSQL_CONTAINER_NAME=\"${MYSQL_CONTAINER_NAME:-lisuan-mysql}\""),
            "容器名应读同一个变量并带同样的默认值");
        assertEquals(2, count(startMysql, "grep -qx \"${MYSQL_CONTAINER_NAME}\""),
            "容器存在性判断（含已停止）都要按变量精确匹配");
        assertTrue(install.contains("MYSQL_CONTAINER_NAME=${MYSQL_CONTAINER_NAME:-\"lisuan-mysql\"}"),
            "install.sh 的同名变量与默认值要保持一致");
    }

    @Test
    @DisplayName("不得再有无人实现的部署开关（DB_USE_SSL）")
    void noUnimplementedDeploymentSwitches() throws Exception {
        for (Path file : new Path[] {ENV_EXAMPLE, CREDENTIALS}) {
            assertFalse(Files.readString(file).contains("DB_USE_SSL"),
                file + " 仍在提 DB_USE_SSL：没有任何代码读它，SSL 实际由 db.url 的 sslMode 决定");
        }
    }

    private static int count(String text, String needle) {
        int total = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            total++;
            index = text.indexOf(needle, index + needle.length());
        }
        return total;
    }
}
