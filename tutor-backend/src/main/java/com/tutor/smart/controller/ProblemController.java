package com.tutor.smart.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tutor.smart.common.BaseResponse;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.common.ResultUtils;
import com.tutor.smart.model.dto.ProblemAddRequest;
import com.tutor.smart.model.dto.ProblemQueryRequest;
import com.tutor.smart.model.entity.Problem;
import com.tutor.smart.model.vo.ProblemVO;
import com.tutor.smart.service.ProblemService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 题目模块控制层
 */
@RestController
@RequestMapping("/problem")
public class ProblemController {

    @Autowired
    private ProblemService problemService;

    /**
     * 新增题目接口
     */
    @PostMapping("/add")
    public BaseResponse<Long> addProblem(@RequestBody ProblemAddRequest problemAddRequest) {
        if (problemAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Problem problem = new Problem();
        BeanUtils.copyProperties(problemAddRequest, problem);

        boolean result = problemService.save(problem);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "新增题目失败");
        }
        return ResultUtils.success(problem.getId(), "新增题目成功");
    }

    /**
     * 分页查询脱敏题目列表接口
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<ProblemVO>> listProblemByPage(@RequestBody ProblemQueryRequest problemQueryRequest) {
        if (problemQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<ProblemVO> problemVOPage = problemService.listProblemVOByPage(problemQueryRequest);
        return ResultUtils.success(problemVOPage);
    }

    /**
     * 根据ID获取题目完整详情
     */
    @GetMapping("/get")
    public BaseResponse<Problem> getProblemById(@RequestParam long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法请求ID");
        }
        Problem problem = problemService.getById(id);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该题目不存在");
        }
        return ResultUtils.success(problem);
    }
}