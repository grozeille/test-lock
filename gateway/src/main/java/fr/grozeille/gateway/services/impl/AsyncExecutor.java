package fr.grozeille.gateway.services.impl;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import fr.grozeille.gateway.model.Job;
import fr.grozeille.gateway.services.impl.ExecutorService;
import fr.grozeille.gateway.services.impl.JpaJobRepository;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class AsyncExecutor implements Runnable {

    private final int executorThreadId;

    private final JpaJobRepository jobRepository;

    private final ExecutorService executorService;

    private final String rootUri;

    public AsyncExecutor(int executorThreadId, JpaJobRepository jobRepository, ExecutorService executorService, String rootUri) {
        this.executorThreadId = executorThreadId;
        this.jobRepository = jobRepository;
        this.executorService = executorService;
        this.rootUri = rootUri;
    }

    @Override
    public void run() {

    }
}