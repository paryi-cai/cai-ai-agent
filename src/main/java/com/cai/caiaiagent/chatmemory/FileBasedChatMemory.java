package com.cai.caiaiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件持久化的对话记忆
 *
 * 背景：默认的内存记忆（InMemoryChatMemory）服务一重启就"失忆"，希望把对话保存到文件/数据库/Redis
 * 实现思路：每个会话（chatId）存一个独立文件，内容是该会话的 Message 列表
 *
 * 为什么用 Kryo 而不是 JSON 序列化？
 * 1. Message 是接口，有 UserMessage / SystemMessage / AssistantMessage 等多种子类，结构不统一
 * 2. 这些子类没有无参构造函数、也没实现 Serializable，JSON 反序列化会失败
 * Kryo 是高性能序列化库，配合 Objenesis 实例化策略可以绕开构造器直接创建对象
 *
 * 升级提示（Spring AI 1.0）：ChatMemory 接口的 get() 不再有 lastN 参数（窗口逻辑移到 MessageWindowChatMemory 中）
 */
public class FileBasedChatMemory implements ChatMemory {

    /** 对话记忆文件保存目录 */
    private final String BASE_DIR;

    /** Kryo 序列化器（static 共享一个实例） */
    private static final Kryo kryo = new Kryo();

    static {
        // 不要求提前注册类（Message 子类太多，逐个注册太麻烦）
        kryo.setRegistrationRequired(false);
        // 实例化策略：Message 子类没有无参构造器，用 Objenesis 绕过构造器创建对象
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    /**
     * 构造对象时指定文件保存目录（不存在会自动创建）
     */
    public FileBasedChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /**
     * 追加消息到指定会话（先读出旧消息，追加后再整体写回）
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversationMessages = getOrCreateConversation(conversationId);
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
    }

    /**
     * 读取指定会话的全部消息
     * 注意：1.0 版本接口不再接收 lastN 参数，读取数量由记忆算法（如 MessageWindowChatMemory）控制
     */
    @Override
    public List<Message> get(String conversationId) {
        return getOrCreateConversation(conversationId);
    }

    /**
     * 清空指定会话（删除对应文件）
     */
    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 从文件读取会话消息（文件不存在则返回空列表）
     */
    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                // 反序列化：把文件内容读回 Message 列表
                messages = kryo.readObject(input, ArrayList.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    /**
     * 把会话消息写入文件（序列化）
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据会话 id 定位文件：每个会话一个 .kryo 文件
     */
    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
