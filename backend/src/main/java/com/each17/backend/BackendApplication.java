package com.each17.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Path;

@SpringBootApplication
@EnableAsync
public class BackendApplication {

    public static void main(String[] args) {
        System.out.println("当前工作目录 (user.dir)    = " + System.getProperty("user.dir"));
        System.out.println("项目所在目录 (user.dir/..) = " + Path.of(".").toAbsolutePath().getParent());
        SpringApplication.run(BackendApplication.class, args);
    }
}