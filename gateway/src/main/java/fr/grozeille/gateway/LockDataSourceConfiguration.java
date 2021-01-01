package fr.grozeille.gateway;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.grozeille.gateway.model.Lambda;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class LockDataSourceConfiguration {

    @Bean(name = "lockDataSourceProperties")
    @ConfigurationProperties("gateway.datasource.lock")
    public DataSourceProperties lockDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "lockDataSource")
    @ConfigurationProperties("gateway.datasource.lock.hikari")
    public DataSource lockDataSource() {
        return lockDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "lockTransactionManager")
    public PlatformTransactionManager lockTransactionManager() {
        return new DataSourceTransactionManager(lockDataSource());
    }
}
