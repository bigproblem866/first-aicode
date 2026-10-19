package com.erp.system.init;

import com.erp.common.core.constant.CommonConstants;
import com.erp.system.config.InitProperties;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时创建初始管理员账号。
 * <p>
 * <b>为什么密码不用 SQL 种子脚本写死</b>：
 * BCrypt 每次加密的盐都不同，哈希值无法用肉眼看对错。如果从网上抄一个
 * {@code $2a$10$...} 写进 data.sql，一旦它对应的明文不是预期的那串，
 * 现象就是「密码明明是 123456 却登录失败」，而这条链路要查
 * （前端 → 过滤器放过 → AuthenticationManager → 密码比对）会耗掉大量时间。
 * 交给 Java 用它自己的 PasswordEncoder 生成，哈希一定是对的。
 * <p>
 * 幂等：账号已存在就什么都不做。所以每次启动都跑没问题。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SysDataInitializer implements ApplicationRunner {

    private final InitProperties initProperties;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!initProperties.isEnabled()) {
            log.info("erp.init.enabled=false，跳过初始管理员初始化");
            return;
        }

        String username = initProperties.getAdminUsername();
        SysUser existing = sysUserMapper.selectUserByUsername(username);
        if (existing != null) {
            log.info("管理员账号 [{}] 已存在，跳过初始化", username);
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername(username);
        admin.setNickName("超级管理员");
        admin.setDeptId(initProperties.getAdminDeptId());
        admin.setSex("2");
        admin.setStatus(CommonConstants.STATUS_NORMAL);
        admin.setRemark("系统初始化自动创建");
        // BCrypt 加密，数据库里存的永远是哈希
        admin.setPassword(passwordEncoder.encode(initProperties.getAdminPassword()));
        sysUserMapper.insert(admin);

        sysUserRoleMapper.insert(admin.getId(), initProperties.getAdminRoleId());

        log.warn("""

                        ==========================================================
                        已创建初始管理员账号
                          账号: {}
                          密码: {}
                        请登录后立即修改密码；生产环境请把 erp.init.enabled 置为 false。
                        ==========================================================""",
                username, initProperties.getAdminPassword());
    }
}
