package com.example.crm.test;

import java.security.SecureRandom;
import java.util.Base64;

public class test {
    public static void main(String[] args) {
        // 1. 生成64位随机字节（转换后≈85位字符串，远超32位要求）
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[64];
        random.nextBytes(randomBytes);

        // 2. 转换为Base64字符串（无特殊字符，适合JWT secret）
        String secret = Base64.getEncoder().encodeToString(randomBytes);

        // 3. 输出结果（直接复制到application.yml的jwt.secret中）
        System.out.println("生成的JWT Secret：" + secret);
        // 示例输出：Y29tLmV4YW1wbGUuY3JtLmp3dC5zZWNyZXQxMjM0NTY3ODkwMTIzNDU2Nzg5MA==
    }
}
