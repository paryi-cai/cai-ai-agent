package com.cai.caiaiagent.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 对话记忆文件清理任务
 *
 * 背景：每个会话（chatId）对应 chat-memory/ 下一个 .kryo 文件，前端"新建会话"会不断产生新
 * chatId，长期运行文件会持续堆积。本任务每天定时清理超过保留期的会话文件。
 *
 * 注意：只在"无活跃会话引用"的前提下删除是安全的——过期阈值（7 天）远大于正常会话生命周期。
 */
@Component
@Slf4j
public class ChatMemoryCleanupTask {

    /** 会话记忆保留天数（超过则清理） */
    private static final int RETENTION_DAYS = 7;

    /** 记忆文件后缀（与 FileBasedChatMemory 中的命名规则保持一致） */
    private static final String MEMORY_FILE_SUFFIX = ".kryo";

    /**
     * 每天凌晨 4 点执行清理
     * （cron 表达式：秒 分 时 日 月 周）
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupExpiredMemoryFiles() {
        File dir = new File(System.getProperty("user.dir") + "/chat-memory");
        if (!dir.isDirectory()) {
            return;
        }
        long deadline = System.currentTimeMillis() - RETENTION_DAYS * 24L * 60 * 60 * 1000;
        File[] expiredFiles = dir.listFiles(file -> file.isFile()
                && file.getName().endsWith(MEMORY_FILE_SUFFIX)
                && file.lastModified() < deadline);
        if (expiredFiles == null || expiredFiles.length == 0) {
            return;
        }
        int deleted = 0;
        for (File file : expiredFiles) {
            if (file.delete()) {
                deleted++;
            }
        }
        log.info("对话记忆清理完成：删除 {} 个超过 {} 天的会话文件", deleted, RETENTION_DAYS);
    }
}
