package show.lmm.tenant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import show.lmm.tenant.config.TenantConfig;
import show.lmm.tenant.utils.sql.mysql.MySqlASTVisitor;

/**
 * mybatis多租户插件自动注册
 *
 * @author liumingming
 * @since 2021-09-02 17:36
 */
@Configuration
@EnableConfigurationProperties(TenantConfig.class)
public class TenantPluginAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TenantPlugin.class)
    public TenantPlugin tenantPlugin() {
        return new TenantPlugin();
    }

    @Bean
    @ConditionalOnMissingBean(TenantPluginBeanPostProcessor.class)
    public TenantPluginBeanPostProcessor tenantPluginBeanPostProcessor() {
        return new TenantPluginBeanPostProcessor();
    }
    @Bean
    @ConditionalOnMissingBean(MySqlASTVisitor.class)
    public MySqlASTVisitor mySqlASTVisitor(TenantConfig tenantConfig) {
        return new MySqlASTVisitor(tenantConfig);
    }
}
