package com.tutor.smart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.mapper.LearningPathMapper;
import com.tutor.smart.mapper.PathNodeMapper;
import com.tutor.smart.model.dto.SubmitRequest;
import com.tutor.smart.model.entity.LearningPath;
import com.tutor.smart.model.entity.PathNode;
import com.tutor.smart.model.entity.Problem;
import com.tutor.smart.model.vo.SubmitVO;
import com.tutor.smart.model.vo.UserVO;
import com.tutor.smart.sandbox.CodeSandbox;
import com.tutor.smart.sandbox.model.ExecuteCodeRequest;
import com.tutor.smart.sandbox.model.ExecuteCodeResponse;
import com.tutor.smart.service.JudgeService;
import com.tutor.smart.service.ProblemService;
import com.tutor.smart.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 判题核心服务实现类
 */
@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    @Autowired
    private ProblemService problemService;

    @Autowired
    private UserService userService;

    @Autowired
    private CodeSandbox codeSandbox;

    @Autowired
    private LearningPathMapper learningPathMapper;

    @Autowired
    private PathNodeMapper pathNodeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 支持事务，防止进度更新出错不回滚
    public SubmitVO submitAndJudge(SubmitRequest submitRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户和题目明细
        UserVO loginUser = userService.getLoginUser(request);
        Problem problem = problemService.getById(submitRequest.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }

        // 2. 构造请求，调用第七阶段写的 Docker 代码沙箱
        ExecuteCodeRequest sandboxRequest = ExecuteCodeRequest.builder()
                .code(submitRequest.getCode())
                .inputCase(problem.getInputCase())
                .language(submitRequest.getLanguage())
                .build();

        log.info("开始调用沙箱编译运行用户代码... problemId: {}", problem.getId());
        ExecuteCodeResponse sandboxResponse = codeSandbox.executeCode(sandboxRequest);
        log.info("沙箱运行结束，状态码: {}", sandboxResponse.getStatus());

        // 3. 构建返回实体
        SubmitVO submitVO = new SubmitVO();
        submitVO.setStatus(sandboxResponse.getStatus());
        submitVO.setOutput(sandboxResponse.getOutput());
        submitVO.setExpectedOutput(problem.getOutputCase());
        submitVO.setErrorMsg(sandboxResponse.getErrorMsg());
        submitVO.setRunTime(sandboxResponse.getRunTime());
        submitVO.setNodeUpdated(false);

        // 4. 比对结果（如果沙箱自身运行无异常，状态为 0 开启比对）
        if (sandboxResponse.getStatus() == 0) {
            String actualOutput = sandboxResponse.getOutput();
            String expectedOutput = problem.getOutputCase();

            // 健壮性比对：清除首尾空格，并将 Windows 换行符 \r\n 统一替换为 Linux 的 \n
            String actualClean = actualOutput.trim().replace("\r\n", "\n");
            String expectedClean = expectedOutput.trim().replace("\r\n", "\n");

            if (actualClean.equals(expectedClean)) {
                submitVO.setStatus(0); // 0 - AC 通过
                log.info("用户代码评测通过 (Accepted)！");

                // 5. 核心联动点：尝试更新用户的路线图节点进度
                tryUpdateUserProgress(loginUser.getId(), problem.getTags(), submitVO);

            } else {
                submitVO.setStatus(1); // 1 - WA 答案错误
                log.info("用户代码评测未通过 (Wrong Answer)！");
            }
        }

        return submitVO;
    }

    /**
     * 智能匹配并更新路线图进度
     */
    private void tryUpdateUserProgress(Long userId, String problemTags, SubmitVO submitVO) {
        if (StringUtils.isBlank(problemTags)) {
            return;
        }

        // 1. 查找用户当前正在学习的路线主表
        QueryWrapper<LearningPath> pathQuery = new QueryWrapper<>();
        pathQuery.eq("user_id", userId);
        List<LearningPath> paths = learningPathMapper.selectList(pathQuery);
        if (paths == null || paths.isEmpty()) {
            return;
        }

        // 题目支持多个标签，分割并逐一尝试匹配
        String[] tags = problemTags.split(",");

        for (LearningPath path : paths) {
            for (String tag : tags) {
                // 2. 匹配未完成的对应阶段节点
                QueryWrapper<PathNode> nodeQuery = new QueryWrapper<>();
                nodeQuery.eq("path_id", path.getId())
                        .eq("matched_tag", tag.trim())
                        .ne("status", 2); // 2 代表已完成，如果不为2，说明可以更新

                PathNode targetNode = pathNodeMapper.selectOne(nodeQuery);
                if (targetNode != null) {
                    // 3. 联动成功：将节点状态更新为 2 (已完成)
                    targetNode.setStatus(2);
                    pathNodeMapper.updateById(targetNode);

                    submitVO.setNodeUpdated(true);
                    submitVO.setUpdatedNodeName(targetNode.getNodeName());
                    log.info("联动成功：已将路线图阶段【{}】的状态自动变更为已完成！", targetNode.getNodeName());
                    return; // 每次通关更新一个匹配节点即可
                }
            }
        }
    }
}