package com.cai.caiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 文件操作工具测试（纯本地、不依赖 Spring 和 AI）
 */
class FileOperationToolTest {

    private final FileOperationTool tool = new FileOperationTool();

    @Test
    @DisplayName("写入文件后读出，内容应一致")
    void testWriteAndReadFile() {
        String fileName = "编程导航-测试.txt";
        String content = "https://www.codefather.cn 程序员编程学习交流社区";
        // 写入
        String writeResult = tool.writeFile(fileName, content);
        System.out.println("写入结果：" + writeResult);
        Assertions.assertTrue(writeResult.contains("successfully"), "写入应该成功");
        // 读取并验证内容一致
        String readResult = tool.readFile(fileName);
        System.out.println("读取结果：" + readResult);
        Assertions.assertEquals(content, readResult, "读出的内容应与写入一致");
    }
}
