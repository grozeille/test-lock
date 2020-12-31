package fr.grozeille.gateway.rest;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import fr.grozeille.gateway.GatewayConfig;
import fr.grozeille.gateway.model.Job;
import fr.grozeille.gateway.model.exceptions.JobNotFoundException;
import fr.grozeille.gateway.model.exceptions.TokenMismatchException;
import fr.grozeille.gateway.services.impl.AsyncExecutor;
import fr.grozeille.gateway.services.impl.ExecutorService;
import fr.grozeille.gateway.services.impl.JpaJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@RestController
@Slf4j
public class GatewayController {

    private final ExecutorService executorService;

    private final JpaJobRepository jobRepository;

    private final GatewayConfig gatewayConfig;

    public GatewayController(ExecutorService executorService, JpaJobRepository jobRepository, GatewayConfig gatewayConfig) {
        this.executorService = executorService;
        this.jobRepository = jobRepository;
        this.gatewayConfig = gatewayConfig;
    }

    @PostMapping("/init")
    public void initData(@RequestParam(defaultValue = "4") Integer poolSize, @RequestParam(defaultValue = "4") Integer lambdaCount) throws Exception {
        log.info("Initialize with fake lambda");
        this.executorService.buildInitData(poolSize, lambdaCount);
    }

    @PostMapping("/lambda/execute/{id}")
    public String execute(@PathVariable String id) throws Exception {
        log.info("Calling Lambda " + id);
        return this.executorService.callLambda(id);
    }

    @PostMapping("/lambda/execute-async/{id}")
    public ResponseEntity<String> executeAsync(@PathVariable String id) throws Exception {
        log.info("Calling Lambda " + id + " async");

        Job job = this.executorService.callLambdaAsync(id);

        UriComponents rootUri = ServletUriComponentsBuilder.fromCurrentContextPath().build();

        // return the url of the job
        return ResponseEntity.accepted().location(URI.create(rootUri.toUriString()+ "/job/"+job.getId())).body(job.getId());
    }

    @GetMapping("/job/refresh/{id}")
    public ResponseEntity<?> refreshJob(@PathVariable String id, @RequestParam String token) throws Exception {
        log.info("Refreshing Lambda " + id);

        try {
            this.executorService.refreshJob(id, token);
        }
        catch (JobNotFoundException jnfe) {
            log.warn("Unable to refresh job", jnfe);
            return ResponseEntity.notFound().build();
        }
        catch (TokenMismatchException tme) {
            log.warn("Unable to refresh job", tme);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/job/{id}")
    public ResponseEntity<Job> getJobResult(@PathVariable String id) throws Exception {
        Optional<Job> jobOptional = this.jobRepository.findById(id);

        if(jobOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(jobOptional.get());
    }
}
