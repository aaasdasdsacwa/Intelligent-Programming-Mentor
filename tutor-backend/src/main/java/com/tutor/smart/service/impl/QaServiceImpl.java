package com.tutor.smart.service.impl;

import cn.hutool.json.JSONUtil;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.model.dto.QaRequest;
import com.tutor.smart.model.vo.UserVO;
import com.tutor.smart.service.QaService;
import com.tutor.smart.service.UserService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 智能问答服务实现类 (高阶上下文 Redis 持久缓存版)
 */
@Service
@Slf4j
public class QaServiceImpl implements QaService {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatLanguageModel chatModel;

    // 🌟 注入 Spring Boot 自带的 Redis 字符串操作模板
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 存储 Redis 对话历史的键名前缀
    private static final String REDIS_CHAT_KEY_PREFIX = "tutor:chat_history:";

    @Override
    public String getAnswer(QaRequest qaRequest, HttpServletRequest request) {
        // 1. 验证用户登录状态并获取当前用户 (确保安全性，获取用户ID用于生成专属Redis Key)
        UserVO loginUser = userService.getLoginUser(request);

        String question = qaRequest.getQuestion();
        if (StringUtils.isBlank(question)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提问内容不能为空");
        }

        log.info("========== 接收到智能 AI 答疑请求: {} ==========", question);
        String redisKey = REDIS_CHAT_KEY_PREFIX + loginUser.getId();
        log.info("当前登录用户 ID: {}, 对应 Redis Key: {}", loginUser.getId(), redisKey);

        // 2. 从 Redis 中读取该用户历史的对话上下文
        List<String> rawHistory = null;
        try {
            rawHistory = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
            log.info("从 Redis 中成功读取到历史原始记录条数: {}", rawHistory == null ? 0 : rawHistory.size());
        } catch (Exception e) {
            log.warn("Redis 读取历史对话记录失败，本次将进行单轮对话: {}", e.getMessage());
        }

        List<ChatMessage> messages = new ArrayList<>();

        // 注入全局系统编程导师设定
        messages.add(SystemMessage.from("你是一位极其耐心且技术精湛的 Java 编程导师。你需要针对学生的编程提问提供清晰、具有启发性的回答，在指出问题的同时鼓励他们思考。"));

        // 如果有历史对话记录，还原上下文对话流
        if (rawHistory != null && !rawHistory.isEmpty()) {
            for (String jsonMsg : rawHistory) {
                try {
                    String role = JSONUtil.parseObj(jsonMsg).getStr("role");
                    String text = JSONUtil.parseObj(jsonMsg).getStr("text");
                    // 使用 LangChain4j 官方推荐的 .from() 静态工厂构建消息
                    if ("user".equals(role)) {
                        messages.add(UserMessage.from(text));
                    } else if ("ai".equals(role)) {
                        messages.add(AiMessage.from(text));
                    }
                } catch (Exception e) {
                    log.error("解析历史对话 JSON 失败: {}", jsonMsg, e);
                }
            }
        }

        // 3. 将用户当前的最新问题拼装加入对话流
        messages.add(UserMessage.from(question));

        log.info("即将送达大模型完整的上下文消息列表 (包含系统设定与历史记录):");
        for (int i = 0; i < messages.size(); i++) {
            log.info("  [消息 {}] -> 角色: {}, 内容: {}", i, messages.get(i).type(), messages.get(i).text());
        }

        // 4. 调用大模型
        String answer;
        try {
            Response<AiMessage> aiResponse = chatModel.generate(messages);
            answer = aiResponse.content().text();
            log.info("大模型回复成功：{}", answer);
        } catch (Exception e) {
            log.error("AI 答疑大模型调用失败: ", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "大模型响应超时，请稍后再试");
        }

        // 5. 将这一轮对话（用户问 & AI答）持久化写入 Redis (最多保留最近 5 轮，1小时过期)
        if (rawHistory != null) {
            try {
                String userJson = JSONUtil.createObj().set("role", "user").set("text", question).toString();
                String aiJson = JSONUtil.createObj().set("role", "ai").set("text", answer).toString();

                stringRedisTemplate.opsForList().rightPush(redisKey, userJson);
                stringRedisTemplate.opsForList().rightPush(redisKey, aiJson);
                stringRedisTemplate.opsForList().trim(redisKey, -10, -1); // 只保留最近 10 条消息 (5轮问答)
                stringRedisTemplate.expire(redisKey, 1, TimeUnit.HOURS); // 设置1小时过期自动清理

                log.info("已成功将最新会话存入 Redis。当前 Key 缓存总长度为: {}", stringRedisTemplate.opsForList().size(redisKey));
            } catch (Exception e) {
                log.warn("保存对话记录到 Redis 失败: {}", e.getMessage());
            }
        }
        log.info("========== 智能 AI 答疑处理完毕 ==========");

        return answer;
    }
}