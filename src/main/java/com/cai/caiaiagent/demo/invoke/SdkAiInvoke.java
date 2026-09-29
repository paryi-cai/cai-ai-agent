package com.cai.caiaiagent.demo.invoke;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

import java.util.Arrays;

/**
 * 方式 1：使用阿里云 DashScope 官方 SDK 调用大模型
 *
 * 特点：类型安全（编译期检查）、错误处理完善，但和特定厂商绑定
 * 运行方式：直接右键运行 main 方法
 */
public class SdkAiInvoke {

    public static GenerationResult callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        // 创建调用客户端
        Generation gen = new Generation();

        // 系统消息（system）：设定 AI 的角色和行为规则
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant.")
                .build();

        // 用户消息（user）：本次提问
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("你是谁？")
                .build();

        // 组装请求参数：密钥、模型名、消息列表、返回格式
        GenerationParam param = GenerationParam.builder()
                .apiKey(TestApiKey.API_KEY)
                .model("qwen-plus")
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        // 发起调用并返回结果
        return gen.call(param);
    }

    public static void main(String[] args) {
        try {
            GenerationResult result = callWithMessage();
            // 转成 JSON 打印，便于观察响应结构（requestId / usage / output 等）
            System.out.println(JsonUtils.toJson(result));
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.err.println("An error occurred while calling the generation service: " + e.getMessage());
        }
        // 主动退出 JVM：SDK 内部有线程池，不主动退出进程会一直挂着
        System.exit(0);
    }
}
