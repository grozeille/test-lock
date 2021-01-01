package fr.grozeille.gateway.services.impl;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import fr.grozeille.gateway.GatewayConfig;
import fr.grozeille.gateway.services.LockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
public class DBLockService implements LockService {

    @Autowired
    private GatewayConfig config;

    @Autowired
    @Qualifier("lockDataSource")
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() throws SQLException {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(config.getLockTimeout());
    }

    @Override
    public void createLambdaLock(String lambdaId) {
    }

    @Override
    public void lockLambda(String lambdaId) {
        HashFunction hf = Hashing.sipHash24();
        //HashFunction hf = Hashing.sha256();
        HashCode hc = hf.newHasher()
                .putString(lambdaId, Charsets.UTF_8)
                .hash();

        log.debug("Getting lock " + hc.asLong() + " ("+lambdaId+")");

        jdbcTemplate.execute("SELECT pg_advisory_lock(" + hc.asLong() + ")");
    }

    @Override
    public void unlockLambda(String lambdaId) {
        HashFunction hf = Hashing.sipHash24();
        //HashFunction hf = Hashing.sha256();
        HashCode hc = hf.newHasher()
                .putString(lambdaId, Charsets.UTF_8)
                .hash();

        log.debug("releasing lock " + hc.asLong()+ " ("+lambdaId+")");
        jdbcTemplate.execute("SELECT pg_advisory_unlock(" + hc.asLong() + ")");
    }

    @Override
    public void lockPool() {
        lockLambda("pool");
    }

    @Override
    public void unlockPool() {
        unlockLambda("pool");
    }
}
