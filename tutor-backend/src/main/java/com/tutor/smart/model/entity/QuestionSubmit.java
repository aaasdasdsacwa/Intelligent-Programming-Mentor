package com.tutor.smart.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat; // 💡 推荐导入，用于格式化 JSON 输出
@Data
@TableName("question_submit")
public class QuestionSubmit implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String language;
    private String code;
    private Integer status;
    private String errorMsg;
    private Long runTime;
    private Long problemId;
    private Long userId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;
}