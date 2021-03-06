package io.openfoundation.spring.cloud.mesh.discovery;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import io.openfoundation.spring.cloud.mesh.MeshPostProcessor;

import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.EnableZoneAffinity;
/**
 * @author henryz
 */
@Configuration
public class MeshDiscoveryClientConfiguration {
    private static final String VALUE_NOT_SET = "__not__set__";

    private static final String DEFAULT_NAMESPACE = "ribbon";

    @Value("${ribbon.client.name}")
    private String serviceId = "client";

    @Value("${spring.cloud.mesh.server.host}")
    private String meshServerHost;

    @Value("${spring.cloud.mesh.server.port}")
    private int meshServerPort;

    public MeshDiscoveryClientConfiguration() {
    }

    public MeshDiscoveryClientConfiguration(String serviceId) {
        this.serviceId = serviceId;
    }

    @Bean
    public MeshPostProcessor meshPostProcessor() {
        return new MeshPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerList<?> ribbonServerList() {
        return new MeshServerList(meshServerHost, meshServerPort);
    }

    @Bean
    public ServerListFilter<Server> ribbonServerListFilter() {
        return new HealthServiceServerListFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public IPing ribbonPing() {
        return new MeshPing();
    }

    @PostConstruct
    public void preProcess() {
        setProp(this.serviceId, DeploymentContextBasedVipAddresses.key(), this.serviceId);
        setProp(this.serviceId, EnableZoneAffinity.key(), "true");
    }

    private void setProp(String serviceId, String suffix, String value) {
        // how to set the namespace properly?
        String key = getKey(serviceId, suffix);
        DynamicStringProperty property = getProperty(key);
        if (property.get().equals(VALUE_NOT_SET)) {
            ConfigurationManager.getConfigInstance().setProperty(key, value);
        }
    }

    private DynamicStringProperty getProperty(String key) {
        return DynamicPropertyFactory.getInstance().getStringProperty(key, VALUE_NOT_SET);
    }

    private String getKey(String serviceId, String suffix) {
        return serviceId + "." + DEFAULT_NAMESPACE + "." + suffix;
    }
}
