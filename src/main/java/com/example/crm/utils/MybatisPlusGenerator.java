package com.example.crm.utils;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import java.io.File;
import java.sql.Types;
import java.util.Collections;

public class MybatisPlusGenerator {
    // 数据库连接配置（适配国内时区）
    static final String url = "jdbc:mysql://localhost:3306/crm?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8&useSSL=false";
    static final String username = "root";
    static final String password = "admin";
    static final String authorName = "example";
    static final String parentPackageNameJava = "com.example.crm";
    static final String tableName = "user";
    static final String mapperXmlPath = System.getProperty("user.dir") + "/src/main/resources/mapper/";

    public static void main(String[] args) {
        // 确保目录存在
        createDirIfNotExists(mapperXmlPath);

        FastAutoGenerator.create(url, username, password)
                // 1.全局配置
                .globalConfig(builder -> {
                    builder.author(authorName)
                            .enableSpringdoc()
                            .disableOpenDir()
                            .outputDir(System.getProperty("user.dir") + "/src/main/java");
                })
                // 2.数据源配置（修正类型转换，使用java.sql.Types）
                .dataSourceConfig(builder -> builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                    int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                    // 替换org.hibernate.type.SqlTypes为java.sql.Types
                    if (typeCode == Types.SMALLINT) { // 正确的JDBC类型常量
                        return DbColumnType.INTEGER;
                    }
                    return typeRegistry.getColumnType(metaInfo);
                }))
                // 3.包名配置
                .packageConfig(builder -> {
                    builder.parent(parentPackageNameJava)
                            .entity("entity")
                            .mapper("mapper")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, mapperXmlPath));
                })
                // 4.策略配置
                .strategyConfig(builder -> {
                    builder.addInclude(tableName)
                            .entityBuilder().enableLombok().enableFileOverride().enableTableFieldAnnotation()
                            .mapperBuilder().enableFileOverride().enableBaseResultMap().enableBaseColumnList()
                            .serviceBuilder().enableFileOverride().formatServiceFileName("%sService")
                            .controllerBuilder().enableFileOverride().enableRestStyle();
                })
                // 5.模板引擎
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        System.out.println("代码生成完成！");
    }

    private static void createDirIfNotExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            boolean mkdirs = dir.mkdirs();
            if (mkdirs) {
                System.out.println("创建目录成功：" + dirPath);
            } else {
                System.err.println("创建目录失败：" + dirPath);
            }
        }
    }
}