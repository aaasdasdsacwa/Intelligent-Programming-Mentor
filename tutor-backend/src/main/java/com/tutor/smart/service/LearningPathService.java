package com.tutor.smart.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tutor.smart.model.dto.PathGenerateRequest;
import com.tutor.smart.model.entity.LearningPath;
import com.tutor.smart.model.entity.PathNode;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 学习路径服务接口
 */
public interface LearningPathService extends IService<LearningPath> {

    /**
     * AI 生成学习路线图并保存到数据库
     * @return 生成的学习路径主表 ID
     */
    long generateLearningPath(PathGenerateRequest generateRequest, HttpServletRequest request);

    /**
     * 获取指定路径下的所有节点列表
     */
    List<PathNode> listNodesByPathId(long pathId);

    /**
     * 更新某个路线节点的状态 (手动勾选完成/切换学习中)
     */
    boolean updateNodeStatus(long nodeId, int status);
}