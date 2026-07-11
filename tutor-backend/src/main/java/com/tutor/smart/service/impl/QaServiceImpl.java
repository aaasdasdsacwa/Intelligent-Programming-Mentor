package com.tutor.smart.service.impl;

import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.model.dto.QaRequest;
import com.tutor.smart.service.QaService;
import com.tutor.smart.service.UserService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AI 智能问答服务实现类
 */
@Service
@Slf4j
public class QaServiceImpl implements QaService {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatLanguageModel chatModel;

    @Override
    public String getAnswer(QaRequest qaRequest, HttpServletRequest request) {
        // 1. 验证用户登录状态（保持安全拦截逻辑）
        userService.getLoginUser(request);

        String question = qaRequest.getQuestion();
        if (StringUtils.isBlank(question)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提问内容不能为空");
        }

        log.info("接收到智能 AI 答疑请求: {}", question);

        try {
            // 直接复用您配置好的 LangChain4j 的 chatModel 进行概念深度回答
            return chatModel.generate(question);
        } catch (Exception e) {
            log.error("AI 答疑生成失败: ", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "大模型响应超时，请稍后再试");
        }
    }
}