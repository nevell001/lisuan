package com.cashier.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 导出工具测试
 */
class ExportUtilTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("测试导出Excel功能")
    void testExportExcel() {
        // 准备测试数据
        String title = "测试导出";
        List<String> headers = List.of("姓名", "年龄", "性别");
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"张三", "25", "男"});
        data.add(new String[]{"李四", "30", "女"});
        data.add(new String[]{"王五", "28", "男"});

        // 导出Excel
        String filePath = ExportUtil.export(title, headers, data, ExportUtil.ExportFormat.EXCEL, "test");

        // 验证导出结果
        assertNotNull(filePath, "导出Excel失败");
        File file = new File(filePath);
        assertTrue(file.exists(), "Excel文件不存在");
        assertTrue(file.length() > 0, "Excel文件为空");
        System.out.println("Excel导出成功: " + filePath);
    }

    @Test
    @DisplayName("测试导出PDF功能")
    void testExportPDF() {
        // 准备测试数据
        String title = "测试导出";
        List<String> headers = List.of("姓名", "年龄", "性别");
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"张三", "25", "男"});
        data.add(new String[]{"李四", "30", "女"});
        data.add(new String[]{"王五", "28", "男"});

        // 导出PDF
        String filePath = ExportUtil.export(title, headers, data, ExportUtil.ExportFormat.PDF, "test");

        // 验证导出结果
        assertNotNull(filePath, "导出PDF失败");
        File file = new File(filePath);
        assertTrue(file.exists(), "PDF文件不存在");
        assertTrue(file.length() > 0, "PDF文件为空");
        System.out.println("PDF导出成功: " + filePath);
    }

    @Test
    @DisplayName("测试导出带时间格式数据")
    void testExportWithDateTime() {
        // 准备测试数据
        String title = "测试时间格式导出";
        List<String> headers = List.of("姓名", "时间");
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"张三", "2026-04-05 12:34:56"});
        data.add(new String[]{"李四", "2026-04-06 09:12:34"});

        // 导出Excel
        String excelPath = ExportUtil.export(title, headers, data, ExportUtil.ExportFormat.EXCEL, "test");
        assertNotNull(excelPath, "导出Excel失败");
        File excelFile = new File(excelPath);
        assertTrue(excelFile.exists(), "Excel文件不存在");

        // 导出PDF
        String pdfPath = ExportUtil.export(title, headers, data, ExportUtil.ExportFormat.PDF, "test");
        assertNotNull(pdfPath, "导出PDF失败");
        File pdfFile = new File(pdfPath);
        assertTrue(pdfFile.exists(), "PDF文件不存在");

        System.out.println("时间格式数据导出成功");
    }

    @Test
    @DisplayName("测试导出空数据")
    void testExportEmptyData() {
        // 准备测试数据
        String title = "测试空数据导出";
        List<String> headers = List.of("姓名", "年龄");
        List<String[]> data = new ArrayList<>();

        // 导出Excel
        String excelPath = ExportUtil.export(title, headers, data, ExportUtil.ExportFormat.EXCEL, "test");
        assertNotNull(excelPath, "导出Excel失败");
        File excelFile = new File(excelPath);
        assertTrue(excelFile.exists(), "Excel文件不存在");

        // 导出PDF
        String pdfPath = ExportUtil.export(title, headers, data, ExportUtil.ExportFormat.PDF, "test");
        assertNotNull(pdfPath, "导出PDF失败");
        File pdfFile = new File(pdfPath);
        assertTrue(pdfFile.exists(), "PDF文件不存在");

        System.out.println("空数据导出成功");
    }
}
