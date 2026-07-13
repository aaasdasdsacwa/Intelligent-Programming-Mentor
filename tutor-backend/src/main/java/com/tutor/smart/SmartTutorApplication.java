package com.tutor.smart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.tutor.smart.mapper") // 告诉 MyBatis 扫描 Mapper 接口的位置
@EnableAsync // 开启异步支持 (后续判题队列和大模型交互会用到)
public class SmartTutorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartTutorApplication.class, args);
        System.out.println("====== 智能编程导师后端服务启动成功 ======");
    }
}