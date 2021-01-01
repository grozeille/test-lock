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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
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

    @Autowired
    @Qualifier("lockTransactionManager")
    private PlatformTransactionManager lockTransactionManager;

    private TransactionTemplate lockTransactionTemplate;

    private RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() throws Exception {
        this.startJobPollingThreads();
        this.lockTransactionTemplate = new TransactionTemplate(this.lockTransactionManager);
        this.lockTransactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    }

    public String callLambda(String lambdaId) throws Exception {
        return this.callLambda(lambdaId, false, null, null);
    }

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

        Container container = getContainerForLambda(lambdaId);

        // call the lambda
        try {
            log.info("Call for lambda " + lambdaId + " on container " + container.getContainerId() + "...");
            return callLambda(container, async, jobId, callbackUrl);
        }
        catch(Exception ex) {
            log.info("Call for lambda " + lambdaId + " on container " + container.getContainerId() + " failed, try again...");
            // if the container is dead, pick another container from the pool...

            container = getContainerForLambda(lambdaId);

            // ...and call it again
            log.info("Call for lambda " + lambdaId + " on container " + container.getContainerId() + "...");
            return callLambda(container, async, jobId, callbackUrl);
        }
    }

    protected Container getContainerForLambda(String lambdaId) throws Exception {

        return lockTransactionTemplate.execute(status -> {
            try {

                Container container = executorPool.getContainerByLambdaId(lambdaId);
                // if there's no container, pick a container from the pool
                if (container == null) {
                    log.info("No container for lambda " + lambdaId + ", try again with lock...");
                    try {
                        lockService.lockLambda(lambdaId);
                    } catch (Exception lex) {
                        throw new Exception("Unable to acquire lock for lambda " + lambdaId, lex);
                    }

                    log.info("Acquired lock for lambda " + lambdaId);
                    try {
                        // try if someone took the lock before us for this lambda
                        container = executorPool.getContainerByLambdaId(lambdaId);
                        if (container == null) {
                            log.info("No container for lambda " + lambdaId + " after lock, fetch free from the pool...");
                            try {

                                // get the first free from the pool, to make it not free anymore, but don't assign to the lambda yet,
                                // until it's completely initialized
                                container = executorPool.assignFirstFree("tentative#"+UUID.randomUUID().toString()+"#"+lambdaId);

                                if (container == null) {
                                    throw new Exception("Unable to find free container for lambda " + lambdaId);
                                }

                                log.info("Lambda " + lambdaId + " associated to container " + container.getContainerId());

                                // simulate the time to upload the source to the container
                                Thread.sleep(3000);

                                // if everything is fine at this stage, we can finally assign the container to the lambda
                                container.setLambdaId(lambdaId);
                                this.executorPool.updateContainer(container);

                            } catch (Exception ex) {
                                throw new Exception("Unable to find free container for lambda " + lambdaId, ex);
                            }
                        } else {
                            log.info("Found container " + container.getContainerId() + " for lambda " + lambdaId);
                        }
                    } finally {
                        lockService.unlockLambda(lambdaId);
                        log.info("Lock for lambda " + lambdaId + " released");
                    }

                } else {
                    log.info("Found container " + container.getContainerId() + " for lambda " + lambdaId);
                }
                return container;

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
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
    public void buildInitData(int poolSize) throws Exception {

        // as of now, we can create only the same number of lambda as the pool size
        // because there's no mechanism to create new container in the pool

        executorPool.initPool(poolSize);

        lambdaRepository.deleteAll();

        for(int cpt = 0; cpt < poolSize; cpt++) {
            createLambda(String.valueOf(cpt+1));
        }
    }

    private void createLambda(String id) throws Exception {
        Lambda l = new Lambda();
        l.setId(id);

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
