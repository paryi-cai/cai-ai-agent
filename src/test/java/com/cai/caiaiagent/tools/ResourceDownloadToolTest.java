package com.cai.caiaiagent.tools;

import com.cai.caiaiagent.constant.FileConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * 资源下载工具测试（需要网络）
 */
class ResourceDownloadToolTest {

    private final ResourceDownloadTool tool = new ResourceDownloadTool();

    @Test
    @DisplayName("下载百度站点图标应保存成功")
    void testDownloadResource() {
        // 用百度站点图标做测试（稳定可访问）
        String url = "https://www.baidu.com/favicon.ico";
        String fileName = "baidu-favicon.ico";
        String result = tool.downloadResource(url, fileName);
        System.out.println("下载结果：" + result);
        Assertions.assertTrue(result.contains("successfully"), "下载应该成功：" + result);
        File file = new File(FileConstant.FILE_SAVE_DIR + "/download/" + fileName);
        Assertions.assertTrue(file.exists() && file.length() > 0, "下载的文件应该存在且非空");
        System.out.println("文件大小：" + file.length() + " 字节");
    }
}
