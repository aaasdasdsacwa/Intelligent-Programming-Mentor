package com.tutor.smart.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 题目分页查询条件
 */
@Data
public class ProblemQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int current = 1;     // 当前页码
    private int pageSize = 10;   // 每页条数
    private String title;        // 标题搜索
    private String difficulty;   // 难度筛选
    private String tags;         // 标签筛选
}