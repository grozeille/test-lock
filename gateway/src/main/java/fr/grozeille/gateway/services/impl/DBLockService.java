package fr.grozeille.gateway.services.impl;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import fr.grozeille.gateway.services.LockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.SQLException;

@Service
@Slf4j
public class DBLockService implements LockService {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() throws SQLException {
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(60);
    }

    @Override
    public void createLambdaLock(String lambdaId) {
    }

    @Override
    public void lockLambda(String lambdaId) {
        HashFunction hf = Hashing.sha256();
        HashCode hc = hf.newHasher()
                .putString(lambdaId, Charsets.UTF_8)
                .hash();

        jdbcTemplate.execute("SELECT pg_advisory_lock(" + hc.asLong() + ")");
    }

    @Override
    public void unlockLambda(String lambdaId) {
        HashFunction hf = Hashing.sha256();
        HashCode hc = hf.newHasher()
                .putString(lambdaId, Charsets.UTF_8)
                .hash();
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
