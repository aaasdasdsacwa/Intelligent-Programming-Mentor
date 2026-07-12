package com.tutor.smart.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.mapper.LearningPathMapper;
import com.tutor.smart.mapper.PathNodeMapper;
import com.tutor.smart.model.dto.PathGenerateRequest;
import com.tutor.smart.model.entity.LearningPath;
import com.tutor.smart.model.entity.PathNode;
import com.tutor.smart.model.vo.UserVO;
import com.tutor.smart.service.LearningPathService;
import com.tutor.smart.service.UserService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import com.tutor.smart.model.entity.PathNode;
/**
 * 学习路径服务实现类
 */
@Service
@Slf4j
public class LearningPathServiceImpl extends ServiceImpl<LearningPathMapper, LearningPath> implements LearningPathService {

    @Autowired
    private UserService userService;

    @Autowired
    private PathNodeMapper pathNodeMapper;

    @Autowired
    private ChatLanguageModel chatModel;
    @Override
    public String getNodeDetail(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1. 查询数据库中该节点的数据
        PathNode node = pathNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "路线节点不存在");
        }

        // 2. 🌟 懒加载核心：如果数据库里该字段不为空，说明已经生成并保存过，直接秒级返回缓存，不再扣除大模型额度
        if (StringUtils.isNotBlank(node.getDetail())) {
            log.info("🎯 命中数据库缓存，直接返回节点【{}】的简单介绍", node.getNodeName());
            return node.getDetail();
        }

        // 3. 如果数据库里没有，则组装指令调用大模型生成
        log.info("🔮 数据库未命中缓存，正在调用 AI 生成节点【{}】的简单介绍...", node.getNodeName());
        String prompt = "你是一位专业的计算机科学导师。请为学生深度科普并讲解技术概念/知识点：【" + node.getNodeName() + "】。\n\n"
                + "要求内容结构条理清晰，包含以下四个维度：\n"
                + "1. 核心定义与基本原理：用通俗易懂的语言解释它是什么、它是如何工作的。\n"
                + "2. 关键应用场景：在真实的工业界开发中，什么情况下我们会使用它。\n"
                + "3. 极简代码示例：给出一个极简、典型的核心代码示例（Java / Python / Go / JS 均可）直观说明其用法。\n"
                + "4. 总结：用一句话提炼它的核心价值。\n\n"
                + "字数严格控制在 500 字左右，段落分明，排版精美。";

        String answer;
        try {
            answer = chatModel.generate(prompt);
        } catch (Exception e) {
            log.error("AI 概念生成失败: ", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "大模型响应超时，请稍后再试");
        }

        // 4. 将 AI 生成的内容更新保存到数据库中，完成首次数据落盘缓存
        node.setDetail(answer);
        pathNodeMapper.updateById(node);

        return answer;
    }
    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务支持
    public long generateLearningPath(PathGenerateRequest generateRequest, HttpServletRequest request) {
        // 1. 获取并验证当前登录用户
        UserVO loginUser = userService.getLoginUser(request);

        String language = generateRequest.getLanguage();
        String target = generateRequest.getTarget();
        String currentLevel = generateRequest.getCurrentLevel();

        if (StringUtils.isAnyBlank(language, target)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学习语言与攻克方向不能为空");
        }

        // 2. 组装高强度的 JSON 强制约束 Prompt（防止 AI 误生成具体题目的名字）
        String systemPrompt = "你是一位精通软件工程的技术专家与教育规划师。你的职责是为用户定制一个由 4 个渐进式核心技术概念/知识点组成的宏观路线图。";
        String userPrompt = "用户基本情况：目前拥有【" + currentLevel + "】的基础，想要学习【" + language + "】编程语言，致力于向【" + target + "】方向发展。\n\n"
                + "请为该用户定制 4 个递进的学习路线阶段（节点），并严格遵守以下核心生成规则：\n"
                + "1. 【属性核心要求】：每一个阶段对象的 \"nodeName\" 必须是该语言中一个【宏观的、核心的技术知识点或概念模块】（例如：\"" + language + " 基础语法\"、\"面向对象编程机制\"、\"数据库与持久化连接\" 等）。\n"
                + "2. 【严禁生成练习题】：绝对不要生成具体的编程练习作业或竞赛题目名称（严禁在 \"nodeName\" 中生成诸如 \"A+B问题\"、\"回文字符串检测\"、\"两数之和\" 等具体题目）！\n"
                + "3. 【关联标签泛化】：请为每个阶段分配一个非常简短、通用的英文技术分类标签 \"matchedTag\"（全小写，单个单词为主，如：\"java\"、\"oop\"、\"string\"、\"web\"、\"algorithm\"），用于和题库中的通用题目分类标签进行联动筛选。\n"
                + "4. 【阶段描述】：阶段描述 \"nodeDesc\" 应该控制在 50 字以内，简明扼要地解释本概念阶段应该掌握哪些宏观知识点，起到学习引导作用。\n\n"
                + "你的输出格式要求极其严格，必须【仅包含】一个规范的 JSON 数组，不需要任何 Markdown 格式包裹标记（绝对不要使用 ```json 或 ``` 标记包裹），直接以 [ 开头，] 结尾。\n"
                + "数组中每个对象的结构如下：\n"
                + "{\n"
                + "  \"nodeName\": \"知识阶段名称，如：面向对象核心机制\",\n"
                + "  \"nodeDesc\": \"本阶段描述，阐述要掌握的宏观概念，50字以内\",\n"
                + "  \"matchedTag\": \"建议匹配的题库分类标签名，全小写，如：oop\",\n"
                + "  \"sequence\": 阶段序号，从 1 到 4\n"
                + "}";

        // 3. 调用 AI 获取路线数据
        log.info("向AI请求生成路线图，language: {}, target: {}", language, target);
        String response;
        try {
            response = chatModel.generate(systemPrompt + "\n" + userPrompt);
        } catch (Exception e) {
            log.error("AI 路线生成失败: ", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 接口调用超时，请稍后再试");
        }

        // 4. 清洗 Markdown 杂质 (防止 AI 强行加 ```json 等标记导致解析失败)
        response = response.trim();
        if (response.startsWith("```")) {
            response = response.substring(response.indexOf("["));
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.lastIndexOf("]") + 1);
        }

        log.info("AI 返回的 JSON 数据: {}", response);

        // 5. 解析并存储数据
        List<PathNode> pathNodes;
        try {
            pathNodes = JSONUtil.toList(response, PathNode.class);
        } catch (Exception e) {
            log.error("JSON 解析失败: ", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回的数据格式有误，请重新尝试生成");
        }

        if (pathNodes == null || pathNodes.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "路径生成失败，AI 未返回有效节点");
        }

        // 保存路径主表
        LearningPath learningPath = new LearningPath();
        learningPath.setUserId(loginUser.getId());
        learningPath.setName(language + " + " + target + " 自定义路线");
        learningPath.setLanguage(language);
        learningPath.setTarget(target);
        this.save(learningPath);

        // 保存明细节点表
        Long pathId = learningPath.getId();
        for (PathNode node : pathNodes) {
            node.setPathId(pathId);
            node.setStatus(0); // 初始状态为 0 - 未开始
            pathNodeMapper.insert(node);
        }

        return pathId;
    }

    @Override
    public List<PathNode> listNodesByPathId(long pathId) {
        if (pathId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<PathNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("path_id", pathId);
        queryWrapper.orderByAsc("sequence");
        return pathNodeMapper.selectList(queryWrapper);
    }

    @Override
    public boolean updateNodeStatus(long nodeId, int status) {
        if (nodeId <= 0 || (status < 0 || status > 2)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PathNode node = pathNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "路线节点不存在");
        }
        node.setStatus(status);
        return pathNodeMapper.updateById(node) > 0;
    }
}