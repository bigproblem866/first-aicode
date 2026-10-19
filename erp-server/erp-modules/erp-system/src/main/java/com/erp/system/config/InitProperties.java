package com.erp.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 初始化配置，对应 application.yml 的 {@code erp.init.*}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "erp.init")
public class InitProperties {

    /** 是否启用初始化。生产环境务必置为 false */
    private boolean enabled = true;

    /** 初始管理员账号 */
    private String adminUsername = "admin";

    /** 初始管理员密码。可用环境变量 ERP_ADMIN_PASSWORD 覆盖 */
    private String adminPassword = "123456";

    /** 初始管理员所属部门，对应 R__seed_system_dept.sql 里的 100（ERP 演示公司） */
    private Long adminDeptId = 100L;

    /** 初始管理员绑定的角色，对应 R__seed_system_role.sql 里的 1（超级管理员） */
    private Long adminRoleId = 1L;
}
