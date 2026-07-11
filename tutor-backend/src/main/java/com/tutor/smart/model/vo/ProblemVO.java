package com.tutor.smart.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目列表展示实体 (安全脱敏)
 */
@Data
public class ProblemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String description;
    private String difficulty;
    private String tags;
    private LocalDateTime createTime;
}