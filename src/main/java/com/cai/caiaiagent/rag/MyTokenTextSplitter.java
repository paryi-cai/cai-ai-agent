package com.cai.caiaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义 Token 文本分割器（ETL 的 Transform 阶段示例）
 *
 * TokenTextSplitter 关键参数：
 * - chunkSize：每个文本块的目标大小（token 数，默认 800）
 * - minChunkSizeChars：文本块最小字符数（默认 350），超过后寻找句子结尾作为断点，避免语义被切断
 * - minChunkLengthToEmbed：小于该长度的块会被丢弃（默认 5）
 * - maxNumChunks：从文本中生成的最大块数（默认 10000）
 * - keepSeparator：是否在块中保留分隔符（如换行符，默认 true）
 *
 * 使用建议：chunk 太大→检索不精准；chunk 太小→语义缺失，需按文档类型灵活调整
 */
@Component
public class MyTokenTextSplitter {

    /** 使用默认参数切分 */
    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    /** 使用自定义参数切分（chunkSize=200, minChunkSizeChars=100, minChunkLengthToEmbed=10, maxNumChunks=5000, keepSeparator=true） */
    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(200, 100, 10, 5000, true);
        return splitter.apply(documents);
    }
}
