package com.erp.framework.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.erp.common.core.constant.CommonConstants;
import com.erp.framework.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 基础字段自动填充。
 * <p>
 * 为什么必须要有这个类：建表 DDL 里 {@code create_time DEFAULT CURRENT_TIMESTAMP} 只能兜住两个时间字段，
 * <b>兜不住 create_by / update_by</b>（数据库不知道当前登录用户是谁）。没有这个类，
 * 所有表的 create_by 会永远是 NULL。
 * <p>
 * 取值来源是 Spring Security 的上下文；在没有登录态的场景（应用启动时的数据初始化、
 * 定时任务）必须走 {@link SecurityUtils#getUsernameOrSystem()}，否则会抛空指针。
 */
@Slf4j
@Component
public class ErpMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String operator = SecurityUtils.getUsernameOrSystem();

        // strictInsertFill 只在字段为 null 时填充，不会覆盖业务代码显式设置的值
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", String.class, operator);
        this.strictInsertFill(metaObject, "updateBy", String.class, operator);
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时不受"非 null 不覆盖"限制：即使前端传了 updateTime，也应以后端时间为准
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateBy", SecurityUtils.getUsernameOrSystem(), metaObject);
    }

    /**
     * 供数据初始化等无登录态场景复用的操作人标识。
     */
    public static String systemOperator() {
        return CommonConstants.SYSTEM_OPERATOR;
    }
}
