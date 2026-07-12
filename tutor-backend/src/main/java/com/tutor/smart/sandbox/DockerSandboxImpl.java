package com.tutor.smart.sandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.tutor.smart.sandbox.model.ExecuteCodeRequest;
import com.tutor.smart.sandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Docker 多语言通用安全代码沙箱实现类（工业级闭环版）
 */
@Service
@Slf4j
public class DockerSandboxImpl implements CodeSandbox {

    @Autowired
    private DockerClient dockerClient;

    // 运行超时设置 5 秒
    private static final long TIME_LIMIT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        String inputCase = executeCodeRequest.getInputCase();
        String language = executeCodeRequest.getLanguage() != null ? executeCodeRequest.getLanguage().toLowerCase() : "java";

        // 🌟 1. 动态映射不同编程语言的文件后缀名、Docker 基础镜像以及容器内的编译执行命令 [1]
        String suffix;
        String dockerImage;
        String runCommand;

        switch (language) {
            case "python":
                suffix = ".py";
                dockerImage = "python:3.10-alpine"; // 动态使用官方轻量 Python 镜像 [1]
                runCommand = "python3 /app/Main.py < /app/input.txt";
                break;
            case "cpp":
                suffix = ".cpp";
                dockerImage = "frolvlad/alpine-gxx";      // 动态使用官方 Alpine GCC 工具链镜像 [1]
                // 在 Linux 容器内部进行编译并运行，100% 避开 Windows 与 Linux 跨系统编译二进制不兼容问题 [1]
                runCommand = "g++ -O3 /app/Main.cpp -o /app/Main && /app/Main < /app/input.txt";
                break;
            case "go":
                suffix = ".go";
                dockerImage = "golang:1.20-alpine"; // 动态使用官方 Go 环境镜像 [1]
                runCommand = "go run /app/Main.go < /app/input.txt";
                break;
            case "javascript":
                suffix = ".js";
                dockerImage = "node:18-alpine";     // 动态使用官方 Node.js 镜像 [1]
                runCommand = "node /app/Main.js < /app/input.txt";
                break;
            case "java":
            default:
                suffix = ".java";
                dockerImage = "openjdk:17-alpine";  // Java 评测镜像
                runCommand = "javac -encoding utf-8 /app/Main.java && java -cp /app Main < /app/input.txt";
                break;
        }

        // 2. 在本地盘符创建隔离的临时文件夹，写入对应的代码源文件 [1]
        String userDir = System.getProperty("user.dir");
        String tempDirPath = userDir + File.separator + "temp" + File.separator + IdUtil.simpleUUID();
        String sourceFilePath = tempDirPath + File.separator + "Main" + suffix; // 动态后缀名 [1]
        FileUtil.writeString(code, sourceFilePath, StandardCharsets.UTF_8);

        // 3. 将标准输入用例写入本地临时文件夹中的 input.txt
        String inputFilePath = tempDirPath + File.separator + "input.txt";
        String safeInput = inputCase != null ? inputCase : "";
        FileUtil.writeString(safeInput, inputFilePath, StandardCharsets.UTF_8);

        String containerId = null;
        try {
            // 将 Windows 本地物理路径（D:\java项目\temp\xxx）转换为 Docker 兼容的 POSIX 路径（/d/java项目/temp/xxx） [1]
            String dockerBindPath = tempDirPath;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                dockerBindPath = dockerBindPath.replace("\\", "/");
                if (dockerBindPath.contains(":")) {
                    String[] split = dockerBindPath.split(":");
                    String driveLetter = split[0].toLowerCase();
                    dockerBindPath = "/" + driveLetter + split[1];
                }
            }

            log.info("【沙箱调度】正在为语言 [{}] 调配 Docker 镜像: {}", language, dockerImage);
            log.info("【沙箱调度】挂载映射路径: {}", dockerBindPath);

            // 配置挂载参数
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(128 * 1024 * 1024L) // 适当放宽内存限制到 128MB 以便容纳 C++/Go 的多编译器运行
                    .withCpuCount(1L)
                    .withNetworkMode("none")       // 禁用网络，防止代码攻击
                    .withBinds(new Bind(dockerBindPath, new Volume("/app")));

            // 创建容器
            CreateContainerResponse container = dockerClient.createContainerCmd(dockerImage)
                    .withHostConfig(hostConfig)
                    .withCmd("sleep", "3600")       // 保持容器常驻运行 [1, 2]
                    .withAttachStdin(false)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)
                    .exec();

            containerId = container.getId();
            // 启动容器
            dockerClient.startContainerCmd(containerId).exec();

            // 4. 在容器内部执行动态拼接的管道指令
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd("sh", "-c", runCommand) // 🌟 核心：统一调起容器内 sh，管道式执行编译与文件输入流重定向
                    .withAttachStdin(false)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            String execId = execCreateCmdResponse.getId();

            // 收集执行流
            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();

            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback(stdoutStream, stderrStream);

            long startTime = System.currentTimeMillis();
            dockerClient.execStartCmd(execId).exec(execStartResultCallback);

            // 5. 等待执行并检查是否超时
            boolean finished = execStartResultCallback.awaitCompletion(TIME_LIMIT, TimeUnit.MILLISECONDS);
            long endTime = System.currentTimeMillis();
            long runTime = endTime - startTime;

            if (!finished) {
                // 超时 (TLE)
                return ExecuteCodeResponse.builder()
                        .status(4) // 4 - TLE
                        .runTime(runTime)
                        .errorMsg("运行超时 (Time Limit Exceeded)")
                        .build();
            }

            String stdoutResult = stdoutStream.toString(StandardCharsets.UTF_8).trim();
            String stderrResult = stderrStream.toString(StandardCharsets.UTF_8).trim();

            if (org.apache.commons.lang3.StringUtils.isNotBlank(stderrResult)) {
                // 运行或编译失败 (RE / CE)
                return ExecuteCodeResponse.builder()
                        .status(3) // 3 - 发生异常
                        .errorMsg(stderrResult)
                        .runTime(runTime)
                        .build();
            }

            // 6. 正常运行完毕，返回 AC/WA 比对容器输出
            return ExecuteCodeResponse.builder()
                    .status(0)
                    .output(stdoutResult)
                    .runTime(runTime)
                    .runMemory(0L)
                    .build();

        } catch (Exception e) {
            log.error("Docker 评测核心异常: ", e);
            return ExecuteCodeResponse.builder().status(3).errorMsg("评测沙箱核心异常: " + e.getMessage()).build();
        } finally {
            // 7. 优雅环境清理：删除临时容器、清理本地文件夹
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    log.error("清理Docker容器失败: {}", containerId);
                }
            }
            FileUtil.del(tempDirPath);
        }
    }
}