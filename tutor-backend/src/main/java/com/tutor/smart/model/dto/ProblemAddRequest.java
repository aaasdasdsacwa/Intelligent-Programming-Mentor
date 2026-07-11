package com.tutor.smart.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 创建题目请求体
 */
@Data
public class ProblemAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String description;
    private String difficulty; // easy / medium / hard
    private String tags;       // "java,collections"
    private String inputCase;  // 评测输入
    private String outputCase; // 评测期望输出
}