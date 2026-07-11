package com.tutor.smart.service.impl;

import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.model.dto.ReviewRequest;
import com.tutor.smart.model.entity.Problem;
import com.tutor.smart.service.ProblemService;
import com.tutor.smart.service.ReviewService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * AI 代码审查与诊断服务实现类
 */
@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ProblemService problemService;

    @Autowired
    private StreamingChatLanguageModel streamingChatModel;

    @Override
    public SseEmitter codeReviewStream(ReviewRequest reviewRequest) {
        // 1. 获取题目上下文
        Problem problem = problemService.getById(reviewRequest.getProblemId());
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "关联题目不存在");
        }

        // 2. 核心：组装高水准的编程导师 Prompt
        String prompt = "你是一位资深的 Java 编程导师，擅长代码诊断与逻辑重构。现在有一道编程题目，请你根据题目信息和用户提交的代码，进行深度的 Code Review 并给出诊断报告。\n\n"
                + "【题目名称】：" + problem.getTitle() + "\n"
                + "【题目描述】：\n" + problem.getDescription() + "\n\n"
                + "【输入样例】：" + problem.getInputCase() + "\n"
                + "【输出样例】：" + problem.getOutputCase() + "\n\n"
                + "【用户编写的 Java 代码】：\n"
                + "```java\n" + reviewRequest.getCode() + "\n```\n\n"
                + (StringUtils.isNotBlank(reviewRequest.getErrorMsg()) ? "【当前报错信息】：\n" + reviewRequest.getErrorMsg() + "\n\n" : "")
                + "请按照以下结构给出诊断报告（使用清晰的 Markdown 语法进行流式回复）：\n"
                + "1. **总体评价**：简述代码的完成度、逻辑亮点。\n"
                + "2. **问题与 Bug 诊断**：指出代码中潜在的问题、死循环风险、逻辑漏洞，或分析报错原因。\n"
                + "3. **复杂度分析**：估计当前代码的时间复杂度和空间复杂度。\n"
                + "4. **代码规范建议**：分析是否符合 Java 命名规范、对象使用是否合理等。\n"
                + "5. **优化与重构启发**：给出具体的重构方向，可以通过伪代码或文字原理进行点拨，**注意：请绝对不要直接提供完整的最终答案代码，引导学生自己思考并修复**。";

        // 3. 开启 SSE 发送通道 (设置超时时间为 2 分钟，防止长连接中断)
        SseEmitter emitter = new SseEmitter(120_000L);

        // 4. 调用大模型并监听流式返回
        streamingChatModel.generate(prompt, new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String token) {
                try {
                    // 实时将 AI 生成的每个字符推送到前端
                    emitter.send(SseEmitter.event().data(token));
                } catch (IOException e) {
                    log.error("SSE 推送字符失败: {}", e.getMessage());
                }
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                try {
                    // 生成完成，结束 SSE 会话
                    emitter.send(SseEmitter.event().name("complete").data(""));
                    emitter.complete();
                    log.info("AI Code Review 生成完毕。");
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("AI 生成发生异常: ", error);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI 诊断服务异常，请稍后再试"));
                } catch (IOException e) {
                    log.error("发送错误事件失败: ", e);
                }
                emitter.completeWithError(error);
            }
        });

        return emitter;
    }
}