package show.lmm.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * 多租户配置
 *
 * @author liumingming
 * @since 2021-09-02 16:32
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mybatis.tenant-plugin", ignoreUnknownFields = false)
public class TenantConfig {

    /**
     * 租户字段名称
     */
    private String fieldName = "tenantId";

    /**
     * 忽略表名列表
     */
    private Set<String> ignoreTableNames = new HashSet<>();
}
