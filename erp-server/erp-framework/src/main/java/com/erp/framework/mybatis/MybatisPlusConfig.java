package com.erp.framework.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件链。顺序有讲究，不要随意调整。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1) 分页插件。DbType 必须显式指定 MySQL：
        //    不指定时框架要自己去连数据库探测方言，第一次分页查询会多一次连接开销。
        //    maxLimit 是硬上限，防止前端传 pageSize=999999 把整库拉出来。
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        // overflow=false：页码超出总页数时返回空列表，而不是跳回第一页
        // （跳回第一页会让"翻到底"看起来像死循环）
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        // 2) 防全表更新/删除。拦下忘写 where 的 update/delete 并直接抛异常。
        //    开发期性价比最高的一个安全网：本项目的逻辑删除是 UPDATE，一旦漏写条件
        //    就是全表 is_deleted=1，无法用备份恢复以外的办法挽回。
        //    注意它会拦住"合法的全表更新"，确实需要时用 UpdateWrapper 显式带上条件。
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }
}
