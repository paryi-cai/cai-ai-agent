package com.cai.caiaiagent.tools;

import com.cai.caiaiagent.constant.FileConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * PDF 生成工具测试（验证中文内容能正常生成）
 */
class PDFGenerationToolTest {

    private final PDFGenerationTool tool = new PDFGenerationTool();

    @Test
    @DisplayName("生成含中文的 PDF 应保存成功且文件非空")
    void testGeneratePDF() {
        String fileName = "七夕约会计划-测试.pdf";
        String content = "七夕约会计划：包含餐厅预订、活动流程和礼物清单。编程导航 https://www.codefather.cn";
        String result = tool.generatePDF(fileName, content);
        System.out.println("生成结果：" + result);
        Assertions.assertTrue(result.contains("successfully"), "PDF 生成应该成功：" + result);
        File file = new File(FileConstant.FILE_SAVE_DIR + "/pdf/" + fileName);
        Assertions.assertTrue(file.exists() && file.length() > 0, "PDF 文件应该存在且非空");
        System.out.println("PDF 文件大小：" + file.length() + " 字节");
    }
}
