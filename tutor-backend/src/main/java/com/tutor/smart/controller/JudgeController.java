package com.tutor.smart.controller;

import com.tutor.smart.common.BaseResponse;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.common.ResultUtils;
import com.tutor.smart.model.dto.SubmitRequest;
import com.tutor.smart.model.vo.SubmitVO;
import com.tutor.smart.sandbox.DockerSandboxImpl;
import com.tutor.smart.sandbox.model.ExecuteCodeRequest;
import com.tutor.smart.sandbox.model.ExecuteCodeResponse;
import com.tutor.smart.service.JudgeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
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

    // 💡 注入已配置好的代码沙箱具体实现类
    @Autowired
    private DockerSandboxImpl dockerSandbox;

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

    /**
     * 🌟 新增：自拟测试运行调试接口
     * 接收前端传来的代码与自拟输入，直接在沙箱中运行并返回输出，不对比标准答案
     */
    @PostMapping("/run")
    public BaseResponse<ExecuteCodeResponse> runCode(@RequestBody RunCodeRequest runCodeRequest) {
        if (runCodeRequest == null || StringUtils.isBlank(runCodeRequest.getCode())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "运行的代码内容不能为空");
        }

        // 1. 构造沙箱执行请求 DTO
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setCode(runCodeRequest.getCode());
        executeCodeRequest.setLanguage(runCodeRequest.getLanguage());

        // 💡 适配转换：前端传来的参数叫 input，沙箱里读取的字段叫 inputCase，在此处做完美桥接
        executeCodeRequest.setInputCase(runCodeRequest.getInput());

        // 2. 直接调用我们刚刚优化好、100% 畅通的 Docker 沙箱执行编译运行
        ExecuteCodeResponse executeCodeResponse = dockerSandbox.executeCode(executeCodeRequest);

        return ResultUtils.success(executeCodeResponse);
    }

    /**
     * 🌟 辅助内部类：用于无缝对接前端传来的 JSON DTO 结构
     */
    @Data
    public static class RunCodeRequest {
        private String code;
        private String language;
        private String input; // 对应前端 customInput
    }
}