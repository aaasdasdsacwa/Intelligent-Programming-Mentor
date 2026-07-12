package com.tutor.smart.model.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SubmitQueryRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private int current = 1;
    private int pageSize = 10;
    private Long problemId;
    private String language;
    private Integer status;
}