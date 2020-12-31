package fr.grozeille.gateway.services.impl;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import fr.grozeille.gateway.GatewayConfig;
import fr.grozeille.gateway.model.Container;
import fr.grozeille.gateway.model.ExecuteRequest;
import fr.grozeille.gateway.model.Job;
import fr.grozeille.gateway.model.Lambda;
import fr.grozeille.gateway.model.exceptions.JobNotFoundException;
import fr.grozeille.gateway.model.exceptions.LambdaNotFoundException;
import fr.grozeille.gateway.model.exceptions.TokenMismatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ExecutorService {

    @Autowired
    private DBLockService lockService;

    @Autowired
    private DBLambdaRepository lambdaRepository;

    @Autowired
    private JpaJobRepository jobRepository;

    @Autowired
    private DBExecutorPool executorPool;

    @Autowired
    private GatewayConfig gatewayConfig;

    private RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() throws Exception {
        this.startJobPollingThreads();
    }

    @Transactional(readOnly = false)
    public String callLambda(String lambdaId) throws Exception {
        return this.callLambda(lambdaId, false, null, null);
    }

    @Transactional(readOnly = false)
    public Job callLambdaAsync(String lambdaId) throws Exception {
        String jobId = UUID.randomUUID().toString();

        // create a new job for the execution
        Job job = new Job();
        job.setSubmitDateTimestamp(System.currentTimeMillis());
        job.setStatus(Job.JobStatus.SUBMITTED);
        job.setId(jobId);
        job.setLambdaId(lambdaId);

        this.jobRepository.save(job);

        return job;
    }

    private String callLambda(String lambdaId, Boolean async, String jobId, String callbackUrl) throws Exception {

        // get the container for this lambda
        Optional<Lambda> lambdaOptional = lambdaRepository.load(lambdaId);
        if(lambdaOptional.isEmpty()) {
            throw new LambdaNotFoundException(lambdaId);
        }
        Lambda lambda = lambdaOptional.get();
        Container container = null;

        // if there's no container, pick a container from the pool
        if(lambda.getContainerId() == null) {
            log.info("No container for lambda " + lambdaId + ", try again with lock...");
            lockService.lockLambda(lambda.getId());
            log.info("Acquired lock for lambda " + lambdaId);
            try {
                // try if someone took the lock before us
                lambda = lambdaRepository.load(lambdaId).get();
                if(lambda.getContainerId() == null) {
                    log.info("No container for lambda " + lambdaId + ", fetch free from the pool...");
                    lockService.lockPool();
                    log.info("Acquired lock for pool for lambda " + lambdaId);
                    try {
                        container = executorPool.getFreeContainer();
                        if(container == null) {
                            throw new Exception("No more free container from the pool");
                        }
                        container.setLambdaId(lambda.getId());
                        executorPool.updateContainer(container);
                        lambda.setContainerId(container.getContainerId());
                        lambdaRepository.save(lambda);
                        log.info("Lambda " + lambdaId + " associated to container " + container.getContainerId());
                        // simulate the time to upload the source to the container
                        Thread.sleep(3000);
                    }
                    finally {
                        lockService.unlockPool();
                        log.info("Lock for pool released by lambda " + lambdaId);
                    }
                }
                else {
                    container = executorPool.getContainer(lambda.getContainerId());
                    log.info("Found container " + container.getContainerId() +  " for lambda " + lambdaId);
                }
            }
            finally {
                lockService.unlockLambda(lambda.getId());
                log.info("Lock for lambda " + lambdaId + " released");
            }

        }
        else {
            container = executorPool.getContainer(lambda.getContainerId());
            log.info("Found container " + container.getContainerId() +  " for lambda " + lambdaId);
        }

        // call the lambda
        try {
            log.info("Call for lambda " + lambda.getId() + " on container " + lambda.getContainerId() + "...");
            return callLambda(container, async, jobId, callbackUrl);
        }
        catch(Exception ex) {
            log.info("Call for lambda " + lambda.getId() + " on container " + lambda.getContainerId() + " failed, try again...");
            // if the container is dead, pick another container from the pool...
            lockService.lockLambda(lambdaId);
            log.info("Acquired lock for lambda " + lambdaId);
            try {
                // try if someone took the lock before us
                lambda = lambdaRepository.load(lambdaId).get();
                if (lambda.getContainerId().equals(lambda.getContainerId())) {
                    log.info("No container for lambda " + lambdaId + ", fetch free from the pool...");
                    lockService.lockPool();
                    log.info("Acquired lock for pool for lambda " + lambdaId);
                    try {
                        container = executorPool.getFreeContainer();
                        if(container == null) {
                            throw new Exception("No more free container from the pool");
                        }
                        container.setLambdaId(lambda.getId());
                        executorPool.updateContainer(container);
                        lambda.setContainerId(container.getContainerId());
                        lambdaRepository.save(lambda);
                        log.info("Lambda " + lambdaId + " associated to container " + container.getContainerId());
                    }
                    finally {
                        lockService.unlockPool();
                        log.info("Lock for pool released by lambda " + lambdaId);
                    }
                }
            }
            finally {
                lockService.unlockLambda(lambda.getId());
                log.info("Lock for lambda " + lambdaId + " released");
            }

            // ...and call it again
            log.info("Call for lambda " + lambda.getId() + " on container " + lambda.getContainerId() + "...");
            return callLambda(container, async, jobId, callbackUrl);

        }
    }

    private String callLambda(Container container, Boolean async, String jobId, String callbackUrl) throws Exception {

        if(!async) {
            ResponseEntity<String> response = restTemplate.postForEntity(this.gatewayConfig.getExecutorUri() + "/execute/" + container.getLambdaId(), null, String.class);

            return response.getBody();
        }
        else {
            /*
            this is the long-polling version

            ResponseEntity<String> response = restTemplate.postForEntity(this.gatewayConfig.getExecutorUri()+"/execute-async/" + container.getLambdaId() + "/" + jobId, null, String.class);

            if(response.getStatusCode() == HttpStatus.ACCEPTED) {
                // max 2h
                Long timeout = System.currentTimeMillis() + (1000 * 60 * 60 * 2);

                while(System.currentTimeMillis() < timeout) {
                    return checkLambdaResult(container.getLambdaId(), jobId);
                }

                throw new Exception("Timeout after 2h");
            }
            else {
                throw new Exception("Unable to submit the lambda for async execution");
            }*/

            ExecuteRequest request = new ExecuteRequest();
            request.setCallbackUrl(callbackUrl);
            restTemplate.postForEntity(this.gatewayConfig.getExecutorUri() + "/execute-async-callback/" + container.getLambdaId() + "/" + jobId, request, String.class);

            return null;
        }
    }

    @Transactional(readOnly = false)
    public void buildInitData(int poolSize, int lambdaCount) throws Exception {

        executorPool.initPool(poolSize);

        lambdaRepository.deleteAll();

        for(int cpt = 0; cpt < lambdaCount; cpt++) {
            createLambda(String.valueOf(cpt+1));
        }
    }

    @Transactional(readOnly = false)
    public void createLambda(String id) throws Exception {
        Lambda l = new Lambda();
        l.setId(id);
        l.setContainerId(null);
        l.setLastUsedTimestamp(null);

        lambdaRepository.save(l);
        lockService.createLambdaLock(l.getId());
    }

    @Transactional(readOnly = false)
    public void refreshJob(String id, String token) throws Exception {
        Optional<Job> jobOptional = this.jobRepository.findById(id);
        if(jobOptional.isPresent()) {
            Job job = jobOptional.get();

            // check the token of this job
            String decodedToken = new String(BaseEncoding.base64().decode(token), StandardCharsets.UTF_8);
            String sha256hex = Hashing.sha256()
                    .hashString(decodedToken, StandardCharsets.UTF_8)
                    .toString();

            if(!sha256hex.equals(job.getTokenHash())) {
                throw new TokenMismatchException(id);
            }

            ResponseEntity<String> response;
            try {
                response = restTemplate.getForEntity(this.gatewayConfig.getExecutorUri() + "/execute-async/" + job.getLambdaId() + "/" + job.getId(), String.class);
                job.setResult(response.getBody());
                job.setEndDateTimestamp(System.currentTimeMillis());
                job.setStatus(Job.JobStatus.ENDED);
                this.jobRepository.save(job);
            }catch (HttpClientErrorException.NotFound httpClientErrorException) {
                log.debug("Not executed yet, try again");
            }
        }
        else {
            throw new JobNotFoundException(id);
        }
    }

    public void startJobPollingThreads() {
        for(int cpt = 0; cpt < this.gatewayConfig.getAsyncThreadpoolSize(); cpt++){
            final int cptFinal = cpt;
            Thread thread = new Thread(() -> {
                log.info("Async lambda call thread "+ (cptFinal+1) +" started...");

                while(true) {

                    Optional<Job> job = Optional.empty();
                    job = jobRepository.poll(System.currentTimeMillis());
                    if(job.isPresent()) {

                        try {
                            String randomUUID = UUID.randomUUID().toString();

                            String base64 = BaseEncoding.base64().encode(randomUUID.getBytes(StandardCharsets.UTF_8));
                            String callbackUrl = this.gatewayConfig.getCallbackPublicUri()+ job.get().getId()+"?token="+base64;

                            String sha256hex = Hashing.sha256()
                                    .hashString(randomUUID, StandardCharsets.UTF_8)
                                    .toString();
                            job.get().setTokenHash(sha256hex);

                            job.get().setStatus(Job.JobStatus.RUNNING);

                            this.callLambda(job.get().getLambdaId(), true, job.get().getId(), callbackUrl);

                        } catch (Exception e) {
                            log.error("Unable to execute Lambda " + job.get().getLambdaId(), e);
                            job.get().setEndDateTimestamp(System.currentTimeMillis());
                            job.get().setStatus(Job.JobStatus.ENDED);
                        }
                        finally {
                            jobRepository.save(job.get());
                        }
                    }
                    else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Error while waiting for next poll", e);
                        }
                    }
                }
            });
            thread.setName("JobThread-"+cpt);
            thread.start();
        }
    }
}
