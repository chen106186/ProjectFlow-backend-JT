package com.jitong.projectflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.jitong.projectflow.**.mapper")
@SpringBootApplication
public class ProjectFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjectFlowApplication.class, args);
    }
}
