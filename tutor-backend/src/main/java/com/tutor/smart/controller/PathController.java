package com.tutor.smart.controller;

import com.tutor.smart.common.BaseResponse;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.common.ResultUtils;
import com.tutor.smart.model.dto.PathGenerateRequest;
import com.tutor.smart.model.entity.LearningPath;
import com.tutor.smart.model.entity.PathNode;
import com.tutor.smart.service.LearningPathService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习路径规划控制器
 */
@RestController
@RequestMapping("/path")
public class PathController {

    @Autowired
    private LearningPathService learningPathService;

    /**
     * 智能生成学习路径
     */
    @PostMapping("/generate")
    public BaseResponse<Long> generatePath(@RequestBody PathGenerateRequest generateRequest, HttpServletRequest request) {
        if (generateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long pathId = learningPathService.generateLearningPath(generateRequest, request);
        return ResultUtils.success(pathId, "智能学习路径生成成功");
    }

    /**
     * 获取学习路径下的节点列表
     */
    @GetMapping("/nodes")
    public BaseResponse<List<PathNode>> getPathNodes(@RequestParam long pathId) {
        if (pathId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<PathNode> nodes = learningPathService.listNodesByPathId(pathId);
        return ResultUtils.success(nodes);
    }

    /**
     * 更新路线节点状态 (如: 完成/学习中)
     */
    @PostMapping("/node/status")
    public BaseResponse<Boolean> updateNodeStatus(@RequestParam long nodeId, @RequestParam int status) {
        boolean result = learningPathService.updateNodeStatus(nodeId, status);
        return ResultUtils.success(result, "状态更新成功");
    }
}