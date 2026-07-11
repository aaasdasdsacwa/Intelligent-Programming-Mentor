package com.tutor.smart.service;

import com.tutor.smart.model.dto.ReviewRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 代码审查与诊断服务接口
 */
public interface ReviewService {

    /**
     * 发起代码 Review (流式返回)
     * @return 用于长连接实时推送的 SseEmitter
     */
    SseEmitter codeReviewStream(ReviewRequest reviewRequest);
}