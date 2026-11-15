package com.cashier.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("支付配置原子写")
class PaymentServiceConfigWriteTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("写入后内容可读，文件只留一份且临时文件被清理")
    void writesAndCleansUpTempFile() throws Exception {
        Path target = tempDir.resolve("payment.properties");
        Properties props = new Properties();
        props.setProperty("wechat.api.key", "merchant-secret");

        PaymentService.writeConfigAtomically(target.toFile(), props);

        assertEquals("merchant-secret", load(target).getProperty("wechat.api.key"));
        try (Stream<Path> files = Files.list(tempDir)) {
            assertEquals(List.of(target.getFileName()), files.map(Path::getFileName).toList(),
                "不得残留 .tmp 文件");
        }
    }

    @Test
    @DisplayName("覆盖已有配置时替换为 600 权限")
    void replacesExistingFileWithOwnerOnlyPermissions() throws Exception {
        Path target = tempDir.resolve("payment.properties");
        Files.writeString(target, "payment.mode=disabled\n");
        if (supportsPosix(target)) {
            Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-r--r--"));
        }

        Properties props = new Properties();
        props.setProperty("payment.mode", "production");
        PaymentService.writeConfigAtomically(target.toFile(), props);

        assertEquals("production", load(target).getProperty("payment.mode"));
        if (supportsPosix(target)) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(target)),
                "商户配置不得被同机其他账号读取");
        }
    }

    private static Properties load(Path file) throws Exception {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }
        return props;
    }

    private static boolean supportsPosix(Path file) throws Exception {
        return Files.getFileStore(file).supportsFileAttributeView(PosixFileAttributeView.class);
    }
}
