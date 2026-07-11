package com.tutor.smart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tutor.smart.mapper.ProblemMapper;
import com.tutor.smart.model.dto.ProblemQueryRequest;
import com.tutor.smart.model.entity.Problem;
import com.tutor.smart.model.vo.ProblemVO;
import com.tutor.smart.service.ProblemService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目服务实现类
 */
@Service
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem> implements ProblemService {

    @Override
    public Page<ProblemVO> listProblemVOByPage(ProblemQueryRequest problemQueryRequest) {
        long current = problemQueryRequest.getCurrent();
        long size = problemQueryRequest.getPageSize();
        String title = problemQueryRequest.getTitle();
        String difficulty = problemQueryRequest.getDifficulty();
        String tags = problemQueryRequest.getTags();

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();

        // 1. 模糊匹配标题
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);

        // 2. 精确匹配难度
        queryWrapper.eq(StringUtils.isNotBlank(difficulty), "difficulty", difficulty);

        // 3. 多标签多条件联合查询 (支持输入以逗号分隔的多个标签筛选，如 "java,basics")
        if (StringUtils.isNotBlank(tags)) {
            String[] tagArray = tags.split(",");
            for (String tag : tagArray) {
                queryWrapper.like("tags", StringUtils.trim(tag));
            }
        }

        // 4. 执行分页查询
        Page<Problem> problemPage = this.page(new Page<>(current, size), queryWrapper);

        // 5. 将结果转换为前端需要的 VO 列表
        Page<ProblemVO> problemVOPage = new Page<>(current, size, problemPage.getTotal());
        List<ProblemVO> problemVOList = problemPage.getRecords().stream()
                .map(this::getProblemVO)
                .collect(Collectors.toList());

        problemVOPage.setRecords(problemVOList);
        return problemVOPage;
    }

    @Override
    public ProblemVO getProblemVO(Problem problem) {
        if (problem == null) {
            return null;
        }
        ProblemVO problemVO = new ProblemVO();
        BeanUtils.copyProperties(problem, problemVO);
        return problemVO;
    }
}