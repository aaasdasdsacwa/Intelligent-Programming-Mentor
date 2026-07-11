package com.tutor.smart.controller;

import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.model.dto.ReviewRequest;
import com.tutor.smart.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 代码诊断控制层
 */
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * 发起流式 AI 代码 Review 诊断
     * 必须配置 produces 为 TEXT_EVENT_STREAM_VALUE
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter codeReviewStream(@RequestBody ReviewRequest reviewRequest) {
        if (reviewRequest == null || reviewRequest.getProblemId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不全");
        }
        return reviewService.codeReviewStream(reviewRequest);
    }
}