package com.example.crm.controller;

import com.example.crm.common.Result;
import com.example.crm.dto.UserLoginDTO;
import com.example.crm.dto.UserRegisterDTO;
import com.example.crm.entity.SysUser;
import com.example.crm.service.SysUserService;
import com.example.crm.service.PermissionService;
import com.example.crm.vo.LoginResponseVO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class AuthController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PermissionService permissionService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expire:86400000}")
    private long jwtExpire;

    // ========== 登录接口（最终优化版） ==========
    @PostMapping("/login")
    public Result<LoginResponseVO> login(@RequestBody UserLoginDTO loginDTO) {
        // 1. 严格的参数校验（避免空指针）
        if (loginDTO == null) {
            log.warn("登录失败：请求体为空");
            return Result.error("请求参数不能为空");
        }
        String loginName = Optional.ofNullable(loginDTO.getLoginName()).map(String::trim).orElse("");
        String password = Optional.ofNullable(loginDTO.getPassword()).map(String::trim).orElse("");

        if (loginName.isEmpty()) {
            log.warn("登录失败：用户名为空");
            return Result.error("用户名不能为空");
        }
        if (password.isEmpty()) {
            log.warn("登录失败：密码为空（用户：{}）", loginName);
            return Result.error("密码不能为空");
        }

        log.info("收到用户登录请求：{}", loginName);

        // 2. 查询用户（优化空值处理）
        SysUser user = sysUserService.getByLoginName(loginName);
        if (user == null) {
            log.warn("登录失败：用户{}不存在", loginName);
            return Result.error("用户名不存在");
        }

        // 3. 密码校验（优雅的空值兜底）
        boolean passwordMatch = false;
        String userPwd = Optional.ofNullable(user.getPassword()).orElse("");
        if (!userPwd.isEmpty()) {
            if (userPwd.length() == 60) {
                // BCrypt加密密码校验（生产环境推荐）
                passwordMatch = passwordEncoder.matches(password, userPwd);
            } else {
                log.warn("警告：用户{}使用明文密码！建议立即修改", loginName);
                passwordMatch = password.equals(userPwd);
            }
        }

        if (!passwordMatch) {
            log.warn("登录失败：用户{}密码错误", loginName);
            return Result.error("密码错误");
        }

        // 4. JWT Token生成（修复临时密钥问题 + 增强容错）
        SecretKey secretKey;
        try {
            // 确保密钥字节数组非空
            byte[] keyBytes = Optional.ofNullable(jwtSecret)
                    .map(s -> s.getBytes(StandardCharsets.UTF_8))
                    .orElseThrow(() -> new WeakKeyException("JWT密钥为空"));
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            log.error("JWT密钥不合法（需≥32字符），当前长度：{}，请修改配置文件！",
                    jwtSecret == null ? 0 : jwtSecret.length());
            return Result.error("服务器配置错误，请联系管理员");
        } catch (Exception e) {
            log.error("JWT密钥初始化失败", e);
            return Result.error("服务器内部错误");
        }

        // 5. 统一Long类型的userId（修复类型不一致，前端兼容id/userId）
        Long userId = Optional.ofNullable(user.getId()).orElse(0L);
        // 角色兜底：确保永远返回非空角色（前端依赖role字段）
        String userRole = Optional.ofNullable(user.getRole()).orElse("sales");

        String token = Jwts.builder()
                .setSubject(loginName)
                .claim("role", userRole)          // 确保角色非空
                .claim("userId", userId)          // 前端userId字段匹配
                .claim("id", userId)              // 兼容前端id字段
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpire))
                .signWith(secretKey)
                .compact();

        log.info("用户{}登录成功，userId: {}，角色：{}，Token有效期：{}小时",
                loginName, userId, userRole, jwtExpire / 3600000);

        // 6. 封装返回结果（删除setId，仅保留userId）
        LoginResponseVO responseVO = new LoginResponseVO();
        responseVO.setLoginName(loginName);
        responseVO.setRole(userRole);          // 非空角色
        responseVO.setUserId(userId);          // 前端userId字段
        // 注释/删除这一行：responseVO.setId(userId);
        responseVO.setToken(token);

        return Result.success("登录成功", responseVO);
    }

    // ========== 注册接口（最终优化版） ==========
    @PostMapping("/register")
    public Result<?> register(@RequestBody UserRegisterDTO registerDTO) {
        // 1. 参数非空校验（核心修复）
        if (registerDTO == null) {
            log.warn("注册失败：请求体为空");
            return Result.error("请求参数不能为空");
        }
        String loginName = Optional.ofNullable(registerDTO.getLoginName()).map(String::trim).orElse("");
        String password = Optional.ofNullable(registerDTO.getPassword()).map(String::trim).orElse("");
        String phone = Optional.ofNullable(registerDTO.getPhone()).map(String::trim).orElse("");

        if (loginName.isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password.isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (phone.isEmpty()) {
            return Result.error("手机号不能为空");
        }

        log.info("收到用户注册请求：{}，手机号：{}", loginName, phone);

        // 2. 重复校验（优化日志）
        if (sysUserService.existsByLoginName(loginName)) {
            log.warn("注册失败：用户名{}已存在", loginName);
            return Result.error("用户名已存在");
        }
        if (sysUserService.existsByPhone(phone)) {
            log.warn("注册失败：手机号{}已注册", phone);
            return Result.error("手机号已注册");
        }

        // 3. 构建用户对象（默认角色+时间戳）
        SysUser user = new SysUser();
        user.setLoginName(loginName);
        user.setPassword(passwordEncoder.encode(password)); // 强制加密存储
        user.setPhone(phone);
        user.setRole("sales"); // 注册默认角色
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        sysUserService.save(user);
        log.info("用户{}注册成功，手机号：{}，默认角色：sales", loginName, phone);
        return Result.success("注册成功");
    }

    // ========== 权限码查询接口（最终优化版） ==========
    @GetMapping("/perms")
    public Result<List<String>> getPermsByUserId(@RequestParam("userId") Long userId) {
        log.info("接收权限码查询请求，userId: {}", userId);

        // 1. 严格的参数校验
        if (userId == null || userId <= 0) {
            log.warn("权限码查询失败：userId无效，值为{}", userId);
            return Result.error("用户ID不能为空且必须为正整数");
        }

        // 2. 查询权限码（兼容空数据，增强日志）
        List<String> permCodes = permissionService.getPermCodesByUserId(userId);
        if (permCodes.isEmpty()) {
            log.info("用户{}暂无配置权限码（请检查角色-菜单关联）", userId);
        } else {
            log.info("用户{}的权限码数量：{}，权限列表：{}", userId, permCodes.size(), permCodes);
        }

        // 3. 返回结果（确保返回空数组而非null，前端无需判空）
        return Result.success("权限码查询成功", permCodes);
    }

    // ========== 用户信息查询接口（最终优化版） ==========
    @GetMapping("/info")
    public Result<SysUser> getUserInfo(@RequestParam("userId") Long userId) {
        log.info("接收用户信息查询请求，userId: {}", userId);

        // 1. 参数校验
        if (userId == null || userId <= 0) {
            log.warn("用户信息查询失败：userId无效，值为{}", userId);
            return Result.error("用户ID不能为空且必须为正整数");
        }

        // 2. 查询用户信息
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            log.warn("用户信息查询失败：userId{}不存在", userId);
            return Result.error("用户不存在");
        }

        // 3. 脱敏+兜底处理（前端安全+字段非空）
        user.setPassword(null); // 隐藏密码
        // 角色兜底：避免前端接收null角色
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("sales");
            log.warn("用户{}角色为空，已兜底为sales", userId);
        }

        log.info("用户{}信息查询成功，角色：{}，手机号：{}",
                userId, user.getRole(), user.getPhone());
        return Result.success("用户信息查询成功", user);
    }
}