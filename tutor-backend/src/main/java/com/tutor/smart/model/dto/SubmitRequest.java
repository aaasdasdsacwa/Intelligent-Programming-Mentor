package com.tutor.smart.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户代码提交请求
 */
@Data
public class SubmitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long problemId;  // 题目 ID
    private String code;     // 用户编写的源码
    private String language; // 编程语言，默认为 java
}