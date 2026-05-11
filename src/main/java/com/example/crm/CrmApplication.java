package com.example.crm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration; // 可选：关闭默认Security自动配置，避免干扰

// 核心：添加 exclude = SecurityAutoConfiguration.class 关闭默认Security（可选，避免生成随机密码干扰）
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@MapperScan(basePackages = "com.example.crm.mapper") // 唯一扫描Mapper的地方，不要加其他扫描注解
public class CrmApplication {
	public static void main(String[] args) {
		SpringApplication.run(CrmApplication.class, args);
	}
}