package com.tutor.smart.sandbox;

import com.tutor.smart.sandbox.model.ExecuteCodeRequest;
import com.tutor.smart.sandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱通用接口 (后续可扩展不同语言的实现)
 */
public interface CodeSandbox {

    /**
     * 核心执行方法
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}