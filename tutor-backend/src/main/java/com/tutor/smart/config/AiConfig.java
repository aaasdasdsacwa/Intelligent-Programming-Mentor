package com.tutor.smart.config;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 大模型流式客户端配置类
 */
@Configuration
public class AiConfig {

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    /**
     * 构建支持流式返回的大模型 Bean
     */
    @Bean
    public StreamingChatLanguageModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.3) // 较低随机度，保证代码诊断的严谨性
                .timeout(Duration.ofSeconds(60))
                .build();
    }
    /**
     * 构建非流式标准对话模型（用于生成结构化路径）
     */
    @org.springframework.context.annotation.Bean
    public dev.langchain4j.model.chat.ChatLanguageModel chatModel() {
        return dev.langchain4j.model.openai.OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}