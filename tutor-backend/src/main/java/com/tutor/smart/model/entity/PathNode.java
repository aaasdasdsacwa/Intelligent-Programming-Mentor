package com.tutor.smart.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 路径节点实体
 */
@Data
@TableName("path_node")
public class PathNode implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long pathId;         // 关联路径主表ID

    private String nodeName;     // 节点名称

    private String nodeDesc;     // 节点描述

    private String matchedTag;   // 关联题库的标签 (如 "java-basics")

    private Integer sequence;    // 排序顺序

    private Integer status;      // 完成状态：0-未开始，1-学习中，2-已完成

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}