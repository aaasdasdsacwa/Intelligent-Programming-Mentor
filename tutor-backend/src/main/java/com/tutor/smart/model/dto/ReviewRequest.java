package com.tutor.smart.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * AI 代码 Review 请求体
 */
@Data
public class ReviewRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long problemId;  // 题目 ID (用于提取题目上下文)
    private String code;     // 用户编写的代码
    private String errorMsg; // 编译或运行报错信息 (可选，若有则传给AI帮改错)
}