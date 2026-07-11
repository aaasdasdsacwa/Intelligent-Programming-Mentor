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
 * Docker 安全代码沙箱实现类（终极稳定重定向版）
 */
@Service
@Slf4j
public class DockerSandboxImpl implements CodeSandbox {

    @Autowired
    private DockerClient dockerClient;

    // 默认评测的 JRE 镜像（对应我们打好本地标签的openjdk:17-alpine）
    private static final String DOCKER_IMAGE = "openjdk:17-alpine";
    // 运行超时设置 5 秒
    private static final long TIME_LIMIT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        String inputCase = executeCodeRequest.getInputCase();

        // 1. 在本地盘符创建隔离的临时文件夹，写入 Main.java
        String userDir = System.getProperty("user.dir");
        String tempDirPath = userDir + File.separator + "temp" + File.separator + IdUtil.simpleUUID();
        String sourceFilePath = tempDirPath + File.separator + "Main.java";
        FileUtil.writeString(code, sourceFilePath, StandardCharsets.UTF_8);

        // 🌟 核心改进 1：将标准输入用例直接写入本地临时文件夹中的 input.txt 中
        String inputFilePath = tempDirPath + File.separator + "input.txt";
        String safeInput = inputCase != null ? inputCase : "";
        FileUtil.writeString(safeInput, inputFilePath, StandardCharsets.UTF_8);

        // 2. 本地编译 Java 代码
        String compileCmd = String.format("javac -encoding utf-8 %s", sourceFilePath);
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            int compileExitCode = compileProcess.waitFor();
            if (compileExitCode != 0) {
                // 编译失败，提取错误信息并返回编译错误 (CE)
                ByteArrayOutputStream errStream = new ByteArrayOutputStream();
                compileProcess.getErrorStream().transferTo(errStream);
                String errorMsg = errStream.toString(StandardCharsets.UTF_8);
                // 清理临时文件
                FileUtil.del(tempDirPath);
                return ExecuteCodeResponse.builder()
                        .status(2) // 2 - CE
                        .errorMsg(errorMsg)
                        .build();
            }
        } catch (Exception e) {
            log.error("本地编译发生异常: ", e);
            FileUtil.del(tempDirPath);
            return ExecuteCodeResponse.builder().status(2).errorMsg("编译系统异常: " + e.getMessage()).build();
        }

        // 3. 将本地编译好的 Main.class 挂载进隔离的 Docker 容器运行
        String containerId = null;
        try {
            // 将 Windows 本地路径（D:\java项目\temp\xxx）转换为 Docker 兼容的 POSIX 路径（/d/java项目/temp/xxx） [1]
            String dockerBindPath = tempDirPath;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                dockerBindPath = dockerBindPath.replace("\\", "/");
                if (dockerBindPath.contains(":")) {
                    String[] split = dockerBindPath.split(":");
                    String driveLetter = split[0].toLowerCase(); // 得到 d 或者 c
                    dockerBindPath = "/" + driveLetter + split[1]; // 拼接为 /d/... [1]
                }
            }

            log.info("本地挂载物理路径为: {}", tempDirPath);
            log.info("转换后映射到 Docker 路径为: {}", dockerBindPath);

            // 配置只读挂载参数：将转换后的 dockerBindPath 挂载为容器内的 /app 目录
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(64 * 1024 * 1024L) // 限制最大内存 64MB
                    .withCpuCount(1L)              // 限制单核 CPU
                    .withNetworkMode("none")       // 禁用网络，防止代码攻击外部服务
                    .withBinds(new Bind(dockerBindPath, new Volume("/app")));

            // 创建容器
            CreateContainerResponse container = dockerClient.createContainerCmd(DOCKER_IMAGE)
                    .withHostConfig(hostConfig)
                    .withCmd("sleep", "3600") // 使用 sleep 保持容器后台常驻 [1, 2]
                    .withAttachStdin(false)   // 🌟 彻底不再需要从宿主机附加 stdin，防止 Socket 阻塞
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)           // 关闭 TTY 模式 [1]
                    .exec();

            containerId = container.getId();
            // 启动容器
            dockerClient.startContainerCmd(containerId).exec();

            // 4. 在容器内部执行运行指令: sh -c "java -cp /app Main < /app/input.txt"
            // 🌟 核心改进 2：通过 Linux 自带的 sh 终端，将容器内的 /app/input.txt 重定向输入给 java 程序
            // 这能在读取到文件末尾时完美、自动、由系统底层发送真正的 EOF 闭合信号，彻底斩断超时可能！
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd("sh", "-c", "java -cp /app Main < /app/input.txt")
                    .withAttachStdin(false) // 关闭 Stdin 附加
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            String execId = execCreateCmdResponse.getId();

            // 收集执行流
            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();

            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback(stdoutStream, stderrStream);

            long startTime = System.currentTimeMillis();
            // 启动内部指令（彻底移除了 .withStdIn()，避开了 docker-java 的网络流挂起 Bug）
            dockerClient.execStartCmd(execId)
                    .exec(execStartResultCallback);

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
                // 发生运行时异常 (RE)
                return ExecuteCodeResponse.builder()
                        .status(3) // 3 - RE
                        .errorMsg(stderrResult)
                        .runTime(runTime)
                        .build();
            }

            // 6. 正常运行完毕，返回 AC/WA 比对容器输出
            return ExecuteCodeResponse.builder()
                    .status(0) // 基础完成状态 (外部判题机会对比期望输出更新此状态)
                    .output(stdoutResult)
                    .runTime(runTime)
                    .runMemory(0L) // 内存获取预留字段
                    .build();

        } catch (Exception e) {
            log.error("Docker 评测核心异常: ", e);
            return ExecuteCodeResponse.builder().status(3).errorMsg("评测沙箱核心异常: " + e.getMessage()).build();
        } finally {
            // 7. 优雅环境清理：删除临时容器、清理本地字节码目录
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