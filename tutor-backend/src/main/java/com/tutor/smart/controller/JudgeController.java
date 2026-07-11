package com.tutor.smart.controller;

import com.tutor.smart.common.BaseResponse;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.common.ResultUtils;
import com.tutor.smart.model.dto.SubmitRequest;
import com.tutor.smart.model.vo.SubmitVO;
import com.tutor.smart.service.JudgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 核心判题服务控制器
 */
@RestController
@RequestMapping("/judge")
public class JudgeController {

    @Autowired
    private JudgeService judgeService;

    /**
     * 答题评测提交接口
     */
    @PostMapping("/submit")
    public BaseResponse<SubmitVO> submitCode(@RequestBody SubmitRequest submitRequest, HttpServletRequest request) {
        if (submitRequest == null || submitRequest.getProblemId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提交参数不能为空");
        }
        SubmitVO submitVO = judgeService.submitAndJudge(submitRequest, request);
        return ResultUtils.success(submitVO, "评测完成");
    }
}