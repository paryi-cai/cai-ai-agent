package com.cai.caiaiagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置：让浏览器里的前端页面能跨域调用后端接口
 *
 * 为什么需要：前端页面（如 http://localhost:5173）和后端服务（http://localhost:8123）
 * 域名/端口不同，属于"跨域"；浏览器同源策略默认拦截，服务端返回 CORS 响应头后才会放行
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有接口路径
        registry.addMapping("/**")
                // 允许携带凭证（Cookie 等）
                .allowCredentials(true)
                // 放行任意来源
                // ★ 注意：这里必须用 allowedOriginPatterns，不能用 allowedOrigins("*")——
                //   后者与 allowCredentials(true) 同时配置会冲突报错（跨域安全限制）
                .allowedOriginPatterns("*")
                // 允许的请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许所有请求头
                .allowedHeaders("*")
                // 暴露所有响应头（前端可读取）
                .exposedHeaders("*");
    }
}
