package com.youlai.boot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.youlai.boot.framework.mybatis.MetaFieldFillHandler;
import com.youlai.boot.framework.mybatis.MyDataPermissionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis-Plus 配置类
 *
 * @author Ray.Hao
 * @since 4.3.1
 */
@Configuration
@EnableTransactionManagement
public class MybatisConfig {

    /**
     * 分页插件和数据权限插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 数据权限
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new MyDataPermissionHandler()));

        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

    /**
     * 自动填充创建时间和更新时间
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(new MetaFieldFillHandler());
        return globalConfig;
    }

}
