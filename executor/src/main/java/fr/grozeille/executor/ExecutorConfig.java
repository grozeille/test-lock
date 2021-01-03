package fr.grozeille.executor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "executor")
@Data
public class ExecutorConfig {
    private long executionTimeMs;

    private int asyncMaxQueue;
}
