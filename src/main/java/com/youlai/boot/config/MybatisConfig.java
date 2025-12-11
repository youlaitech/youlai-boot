package com.youlai.boot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.youlai.boot.config.property.TenantProperties;
import com.youlai.boot.plugin.mybatis.MyDataPermissionHandler;
import com.youlai.boot.plugin.mybatis.MyMetaObjectHandler;
import com.youlai.boot.plugin.mybatis.MyTenantLineHandler;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Properties;

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

    @Autowired(required = false)
    private MyTenantLineHandler myTenantLineHandler;

    @Autowired(required = false)
    private TenantProperties tenantProperties;

    /**
     * 分页插件和数据权限插件
     * <p>
     * 如果启用了多租户，则添加多租户插件（必须在最前面）
     * </p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 多租户插件（如果启用，必须在最前面）
        if (tenantProperties != null && Boolean.TRUE.equals(tenantProperties.getEnabled()) && myTenantLineHandler != null) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(myTenantLineHandler));
        }

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

    /**
     * 数据库类型自动识别
     */
    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("DM", "dm");
        properties.setProperty("MySQL", "mysql");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }

}
