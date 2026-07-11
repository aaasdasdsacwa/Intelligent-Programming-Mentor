package com.tutor.smart.model.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 学习路径生成请求体
 */
@Data
public class PathGenerateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String language;     // 想学的语言，如 "Java"
    private String target;       // 攻克方向，如 "Web 后端"
    private String currentLevel; // 当前基础，如 "零基础"、"有C语言基础"
}