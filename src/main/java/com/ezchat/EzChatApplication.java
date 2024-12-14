package com.ezchat;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = "com.ezchat")
@MapperScan(basePackages = {"com.ezchat.mappers"})
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class EzChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(EzChatApplication.class, args);
    }
}
