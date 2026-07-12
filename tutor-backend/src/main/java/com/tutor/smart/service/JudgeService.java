package com.tutor.smart.service;

import com.tutor.smart.model.dto.SubmitRequest;
import com.tutor.smart.model.vo.SubmitVO;
import jakarta.servlet.http.HttpServletRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tutor.smart.model.dto.SubmitQueryRequest;
import com.tutor.smart.model.vo.SubmitVO;
/**
 * 判题核心服务接口
 */
public interface JudgeService {

    /**
     * 提交代码并评测，同时联动更新学习进度
     */
    SubmitVO submitAndJudge(SubmitRequest submitRequest, HttpServletRequest request);
    Page<SubmitVO> listSubmitByPage(SubmitQueryRequest queryRequest, HttpServletRequest request);
}