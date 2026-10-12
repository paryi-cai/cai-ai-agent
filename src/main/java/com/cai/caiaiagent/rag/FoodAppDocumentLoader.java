package com.cai.caiaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载器 —— ETL 流程的第一步：Extract（抽取）
 *
 * RAG 的第一步是把知识库文档读进程序：
 * 1. 扫描 classpath:document/ 下的所有 Markdown 文档（深圳美食知识库）
 * 2. 用 MarkdownDocumentReader 解析成 Document 列表（自动按配置切分成文档块）
 * 3. 给每个文档块附加元信息（来源文件名 + 区域），便于后续过滤/溯源
 */
@Component
@Slf4j
class FoodAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    FoodAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有 Markdown 文档并转换为 Document 列表
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            // 读取 classpath:document/ 下所有 .md 文件（按需修改路径模式）
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                // 元数据标注：从文件名推断"区域"标签，后续可基于它做过滤检索
                String region = resolveRegion(fileName);
                // 文档读取配置：
                // 1. withHorizontalRuleCreateDocument(true)：遇到 --- 水平线就切成独立文档块
                //    （我们的知识文档用 --- 分隔每一组问答，正好一问答 = 一个切片）
                // 2. withIncludeCodeBlock(false)：不把代码块当文档内容
                // 3. withIncludeBlockquote(false)：不把引用块当文档内容
                // 4. withAdditionalMetadata：附加元信息（来源文件名 + 区域），后续可用于过滤和溯源
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", fileName)
                        .withAdditionalMetadata("region", region)
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        log.info("共加载文档切片数：{}", allDocuments.size());
        return allDocuments;
    }

    /**
     * 根据文件名推断"区域"标签（元数据标注）
     * food-qa-nanshan → 南山；food-qa-futian → 福田；
     * food-qa-luohu → 罗湖；food-qa-baoan → 宝安；其余 → 通用
     */
    private String resolveRegion(String fileName) {
        if (fileName == null) {
            return "通用";
        }
        if (fileName.contains("nanshan")) {
            return "南山";
        }
        if (fileName.contains("futian")) {
            return "福田";
        }
        if (fileName.contains("luohu")) {
            return "罗湖";
        }
        if (fileName.contains("baoan")) {
            return "宝安";
        }
        return "通用";
    }
}
