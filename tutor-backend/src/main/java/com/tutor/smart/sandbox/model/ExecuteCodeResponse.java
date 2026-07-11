package com.tutor.smart.sandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 沙箱代码执行响应体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String output;       // 运行标准输出 stdout
    private Integer status;      // 状态：0-AC(通过), 1-WA(答案错误), 2-CE(编译错误), 3-RE(运行时异常), 4-TLE(超时)
    private String errorMsg;     // 报错信息 (stderr 或 compile message)
    private Long runTime;        // 运行耗时 (ms)
    private Long runMemory;      // 运行内存 (byte)
}