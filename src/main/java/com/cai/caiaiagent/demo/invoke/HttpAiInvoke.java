package com.cai.caiaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 方式 2：把大模型当作普通 HTTP 接口来调用（用 Hutool 发请求）
 *
 * 特点：不依赖任何 AI 框架、最灵活，但要自己拼 JSON、解析响应
 * 适合场景：SDK 不支持的语言、临时验证、理解"调用大模型本质上就是一次 HTTP 请求"
 * 运行方式：直接右键运行 main 方法
 */
public class HttpAiInvoke {

    public static void main(String[] args) {
        // 通义千问"文本生成"REST API 地址（原生 DashScope 协议，非 OpenAI 兼容格式）
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        // 请求头：Bearer + API Key 鉴权
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + TestApiKey.API_KEY);
        headers.put("Content-Type", "application/json");

        // 请求体整体结构：{ model, input: { messages }, parameters }
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-plus");

        // input.messages：消息列表（system 设定角色 + user 提问）
        JSONObject input = new JSONObject();
        JSONObject[] messages = new JSONObject[2];

        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a helpful assistant.");
        messages[0] = systemMessage;

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "你是谁？");
        messages[1] = userMessage;

        input.put("messages", messages);
        requestBody.put("input", input);

        // parameters.result_format=message：让响应结构与 SDK 的 message 格式一致，方便阅读
        JSONObject parameters = new JSONObject();
        parameters.put("result_format", "message");
        requestBody.put("parameters", parameters);

        // 发送 POST 请求并获取响应
        HttpResponse response = HttpRequest.post(url)
                .addHeaders(headers)
                .body(requestBody.toString())
                .execute();

        // 按 HTTP 状态码判断成功/失败
        if (response.isOk()) {
            System.out.println("请求成功，响应内容：");
            System.out.println(response.body());
        } else {
            System.out.println("请求失败，状态码：" + response.getStatus());
            System.out.println("响应内容：" + response.body());
        }
    }
}
