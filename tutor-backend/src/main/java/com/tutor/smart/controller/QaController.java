package com.tutor.smart.controller;

import com.tutor.smart.common.BaseResponse;
import com.tutor.smart.common.ResultUtils;
import com.tutor.smart.model.dto.QaRequest;
import com.tutor.smart.service.QaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AI 智能问答与概念科普 API
 */
@RestController
@RequestMapping("/qa") // 💡 此处去掉了多余的 /api，自动应用全局的 context-path [/api]
public class QaController {

    private final QaService qaService;

    @Autowired
    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/chat")
    public BaseResponse<String> chat(@RequestBody QaRequest qaRequest, HttpServletRequest request) {
        String answer = qaService.getAnswer(qaRequest, request);
        return ResultUtils.success(answer);
    }
}