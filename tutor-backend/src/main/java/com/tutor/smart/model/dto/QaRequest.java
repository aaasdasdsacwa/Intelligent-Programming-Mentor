package com.tutor.smart.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 问答请求入参
 */
@Data
public class QaRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提问内容
     */
    private String question;
}