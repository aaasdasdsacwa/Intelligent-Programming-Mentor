package com.tutor.smart.service;

import com.tutor.smart.model.dto.QaRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AI 智能问答服务接口
 */
public interface QaService {

    /**
     * 获取 AI 答疑回复
     */
    String getAnswer(QaRequest qaRequest, HttpServletRequest request);
}