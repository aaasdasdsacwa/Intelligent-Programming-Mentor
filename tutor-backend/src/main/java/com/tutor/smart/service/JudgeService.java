package com.tutor.smart.service;

import com.tutor.smart.model.dto.SubmitRequest;
import com.tutor.smart.model.vo.SubmitVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 判题核心服务接口
 */
public interface JudgeService {

    /**
     * 提交代码并评测，同时联动更新学习进度
     */
    SubmitVO submitAndJudge(SubmitRequest submitRequest, HttpServletRequest request);
}