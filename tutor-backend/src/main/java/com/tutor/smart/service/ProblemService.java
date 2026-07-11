package com.tutor.smart.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tutor.smart.model.dto.ProblemQueryRequest;
import com.tutor.smart.model.entity.Problem;
import com.tutor.smart.model.vo.ProblemVO;

/**
 * 题目服务接口
 */
public interface ProblemService extends IService<Problem> {

    /**
     * 分页多条件查询脱敏后的题目列表
     */
    Page<ProblemVO> listProblemVOByPage(ProblemQueryRequest problemQueryRequest);

    /**
     * 将物理题目实体转化为展示视图对象 (脱敏)
     */
    ProblemVO getProblemVO(Problem problem);
}