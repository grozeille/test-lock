package fr.grozeille.gateway;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gateway")
@Data
public class GatewayConfig {
    private String callbackPublicUri;

    private Integer asyncThreadpoolSize;

    private String executorUri;

    private Integer lockTimeout;
}
