package com.cashier.util;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取与维护工作目录下的 {@code .env}（{@code KEY=VALUE} 文本，{@code #} 开头为注释）。
 *
 * <p>启动脚本只把 {@code .env} 注入进程环境，而 {@code java -jar} 直接运行时不会经过脚本，
 * 因此应用自身也需要能读它——否则密码只能写回 {@code config/database.properties}，
 * 既分叉了配置来源，又会被发布门禁（禁止 {@code config/} 里出现 {@code db.password}）拒绝。</p>
 *
 * <p>读取优先级：真实环境变量 → {@code .env}。写入只发生在安装/配置工具中，
 * 且只写 {@code .env}（已 gitignore），不会写进 {@code config/}。</p>
 */
public final class DotEnv {

    /** 本地密钥文件名（相对工作目录），已在 .gitignore 中排除。 */
    public static final String FILE_NAME = ".env";

    /** 应用数据库密码变量名，与启动脚本、docker-compose、文档保持一致。 */
    public static final String DB_PASSWORD_KEY = "CASHIER_DB_PASSWORD";

    private static final Logger logger = LoggerFactoryUtil.getLogger(DotEnv.class);

    private static Map<String, String> cachedValues;

    private DotEnv() {
    }

    /**
     * 取配置值：优先真实环境变量，其次 {@code .env}。
     *
     * @return 值；两处都没有时返回 null
     */
    public static String get(String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isEmpty()) {
            return fromEnv;
        }
        return load().get(key);
    }

    /**
     * 把配置写进 {@code .env}：已存在则就地替换（重复键只保留一条），不存在则追加。
     * 其它键与注释保持原样。
     */
    public static void upsert(String key, String value) throws IOException {
        upsert(Path.of(FILE_NAME), key, value);
    }

    static void upsert(Path file, String key, String value) throws IOException {
        List<String> lines = Files.exists(file)
            ? new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8))
            : new ArrayList<>();

        String entry = key + "=" + value;
        int replacedAt = -1;
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals <= 0 || !trimmed.substring(0, equals).trim().equals(key)) {
                continue;
            }
            if (replacedAt < 0) {
                lines.set(i, entry);
                replacedAt = i;
            } else {
                lines.remove(i--);
            }
        }
        if (replacedAt < 0) {
            lines.add(entry);
        }

        Files.write(file, lines, StandardCharsets.UTF_8);
        restrictToOwner(file);
        synchronized (DotEnv.class) {
            cachedValues = null; // 下次读取重新加载
        }
    }

    /**
     * 尽量把文件权限收紧到仅属主可读写（Windows 无 POSIX 权限，静默跳过）。
     */
    private static void restrictToOwner(Path file) {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException e) {
            logger.debug("无法收紧 {} 权限（当前平台不支持 POSIX 权限）", file);
        }
    }

    private static synchronized Map<String, String> load() {
        if (cachedValues != null) {
            return cachedValues;
        }
        Path file = Path.of(FILE_NAME);
        Map<String, String> parsed = new HashMap<>();
        if (Files.isReadable(file)) {
            try {
                parsed.putAll(parse(Files.readAllLines(file, StandardCharsets.UTF_8)));
                logger.debug("已加载 {}，共 {} 项", FILE_NAME, parsed.size());
            } catch (IOException e) {
                logger.warn("读取 {} 失败: {}", FILE_NAME, e.getMessage());
            }
        }
        cachedValues = Collections.unmodifiableMap(parsed);
        return cachedValues;
    }

    /**
     * 解析 {@code .env} 内容：忽略空行与 {@code #} 注释，去掉值两端成对的引号，
     * 同一个键出现多次时以第一条为准（与启动脚本 {@code set} 只设一次的行为一致）。
     */
    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = trimmed.substring(0, equals).trim();
            if (key.isEmpty()) {
                continue;
            }
            values.putIfAbsent(key, unquote(trimmed.substring(equals + 1).trim()));
        }
        return values;
    }

    private static String unquote(String value) {
        boolean quoted = value.length() >= 2
            && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")));
        return quoted ? value.substring(1, value.length() - 1) : value;
    }
}
