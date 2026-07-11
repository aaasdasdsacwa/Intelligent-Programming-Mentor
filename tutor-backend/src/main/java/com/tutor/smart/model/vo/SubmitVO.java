package com.tutor.smart.model.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 评测结果响应视图
 */
@Data
public class SubmitVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer status;          // 评测状态：0-AC(通过), 1-WA(答案错误), 2-CE, 3-RE, 4-TLE
    private String output;           // 用户代码的实际输出
    private String expectedOutput;   // 题目的标准期望输出
    private String errorMsg;         // 报错信息
    private Long runTime;            // 运行耗时 (ms)

    // 联动进度字段
    private Boolean nodeUpdated;     // 本次通过是否触发了学习路线节点的进度更新
    private String updatedNodeName;  // 被更新通关的路线阶段节点名称
}