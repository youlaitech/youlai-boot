package com.youlai.boot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.youlai.boot.plugin.mybatis.MyDataPermissionHandler;
import com.youlai.boot.plugin.mybatis.MyMetaObjectHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * mybatis-plus 配置类
 *
 * @author Ray.Hao
 * @since 2022/7/2
 */
@Configuration
@EnableTransactionManagement
public class MybatisConfig {

    @Value("${app.db-type:mysql}")
    private String dbType;

    /**
     * 分页插件和数据权限插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 数据权限
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new MyDataPermissionHandler()));
        // 分页插件，根据配置动态选择数据库类型
        DbType mpDbType = DbType.MYSQL;
        String type = dbType == null ? "mysql" : dbType.toLowerCase();
        if ("postgres".equals(type) || "postgresql".equals(type)) {
            mpDbType = DbType.POSTGRE_SQL;
        } else if ("dm".equals(type) || "dameng".equals(type)) {
            // 达梦更接近 Oracle 语法，这里选择 ORACLE 方言以获得较好兼容性
            mpDbType = DbType.ORACLE;
        }
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(mpDbType));

        return interceptor;
    }

    /**
     * 自动填充数据库创建人、创建时间、更新人、更新时间
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setMetaObjectHandler(new MyMetaObjectHandler());
        return globalConfig;
    }

}
