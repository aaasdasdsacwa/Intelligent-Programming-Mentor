package com.tutor.smart.sandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 沙箱代码执行请求体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;         // 用户编写的Java源码
    private String inputCase;    // 测试输入用例
    private String language;     // 编程语言 (如: java)
}