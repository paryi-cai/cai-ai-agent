package com.cai.imagesearchmcp.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索工具（MCP 服务提供的核心工具）
 *
 * 说明：教程原方案使用 Pexels 图片网站 API，但 Pexels 已暂停新 API Key 签发（2026），
 * 因此改用 SearchAPI 的 Bing 图片搜索接口（https://www.searchapi.io/，复用项目已有 Key）
 * 返回图片原图 URL 列表，供 AI 展示或下载
 */
@Service
public class ImageSearchTool {

    /** SearchAPI 统一搜索接口（通过 engine 参数指定搜索引擎） */
    private static final String API_URL = "https://www.searchapi.io/api/v1/search";

    /** 使用的搜索引擎：Bing 图片搜索 */
    private static final String ENGINE = "bing_images";

    /** SearchAPI Key（环境变量 IMAGE_SEARCH_API_KEY 或 application-local.yml 配置） */
    @Value("${image-search.api-key:}")
    private String apiKey;

    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            return String.join(",", searchImages(query));
        } catch (Exception e) {
            return "Error search image: " + e.getMessage();
        }
    }

    /**
     * 搜索图片 URL 列表（取原图 original.link）
     */
    public List<String> searchImages(String query) {
        if (StrUtil.isBlank(apiKey)) {
            throw new IllegalStateException("SearchAPI Key 未配置（请设置环境变量 IMAGE_SEARCH_API_KEY）");
        }
        // 请求参数：引擎 + 关键词 + Key
        Map<String, Object> params = new HashMap<>();
        params.put("engine", ENGINE);
        params.put("q", query);
        params.put("api_key", apiKey);
        // 发送 GET 请求（参数自动拼接到 URL）
        String response = HttpUtil.get(API_URL, params);
        // 解析响应
        JSONObject json = JSONUtil.parseObj(response);
        // 接口出错时会返回 error 字段（如 "Google Images didn't return any results."）
        String error = json.getStr("error");
        if (StrUtil.isNotBlank(error)) {
            throw new IllegalStateException("图片搜索失败：" + error);
        }
        // 解析响应：images[].original.link
        if (json.getJSONArray("images") == null) {
            throw new IllegalStateException("图片搜索未返回结果："
                    + StrUtil.maxLength(response, 300));
        }
        return json.getJSONArray("images")
                .stream()
                .map(imgObj -> (JSONObject) imgObj)
                .map(imgObj -> imgObj.getJSONObject("original"))
                .map(original -> original.getStr("link"))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }
}
