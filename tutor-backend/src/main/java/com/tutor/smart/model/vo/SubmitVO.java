package com.tutor.smart.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat; // 💡 推荐导入，用于格式化 JSON 输出
/**
 * 评测结果响应视图
 */
@Data
public class SubmitVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String problemTitle; // 关联题目标题，用于列表显示
    private Long id;                  // 提交编号
    private Long problemId;           // 关联题目编号
    private String language;          // 使用的编程语言
    private String code;              // 学生当时提交的代码源码
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime; // 评测提交时间

    private Integer status;          // 评测状态：0-AC(通过), 1-WA(答案错误), 2-CE, 3-RE, 4-TLE
    private String output;           // 用户代码的实际输出
    private String expectedOutput;   // 题目的标准期望输出
    private String errorMsg;         // 报错信息
    private Long runTime;            // 运行耗时 (ms)

    // 联动进度字段
    private Boolean nodeUpdated;     // 本次通过是否触发了学习路线节点的进度更新
    private String updatedNodeName;  // 被更新通关的路线阶段节点名称
}