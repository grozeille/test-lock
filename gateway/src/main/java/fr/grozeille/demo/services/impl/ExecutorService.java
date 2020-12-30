package fr.grozeille.demo.services.impl;

import fr.grozeille.demo.model.Container;
import fr.grozeille.demo.model.Lambda;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class ExecutorService {

    @Autowired
    private DBLockService lockService;

    @Autowired
    private DBLambdaRepository lambdaRepository;

    @Autowired
    private DBExecutorPool executorPool;

    private RestTemplate restTemplate = new RestTemplate();

    @Transactional(readOnly = false)
    public String callLambda(String lambdaId) throws Exception {
        return this.callLambda(lambdaId, false, null);
    }

    @Transactional(readOnly = false)
    public String callLambdaAsync(String lambdaId, String jobId) throws Exception {
        return this.callLambda(lambdaId, true, jobId);
    }

    @Transactional(readOnly = false)
    public String callLambda(String lambdaId, Boolean async, String jobId) throws Exception {

        // get the container for this lambda
        Lambda lambda = lambdaRepository.load(lambdaId);
        Container container = null;

        // if there's no container, pick a container from the pool
        if(lambda.getContainerId() == null) {
            log.info("No container for lambda " + lambdaId + ", try again with lock...");
            lockService.lockLambda(lambda.getId());
            log.info("Acquired lock for lambda " + lambdaId);
            try {
                // try if someone took the lock before us
                lambda = lambdaRepository.load(lambdaId);
                if(lambda.getContainerId() == null) {
                    log.info("No container for lambda " + lambdaId + ", fetch free from the pool...");
                    lockService.lockPool();
                    log.info("Acquired lock for pool for lambda " + lambdaId);
                    try {
                        container = executorPool.getFreeContainer();
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
            return callLambda(container, async, jobId);
        }
        catch(Exception ex) {
            log.info("Call for lambda " + lambda.getId() + " on container " + lambda.getContainerId() + " failed, try again...");
            // if the container is dead, pick another container from the pool...
            lockService.lockLambda(lambdaId);
            log.info("Acquired lock for lambda " + lambdaId);
            try {
                // try if someone took the lock before us
                lambda = lambdaRepository.load(lambdaId);
                if (lambda.getContainerId().equals(lambda.getContainerId())) {
                    log.info("No container for lambda " + lambdaId + ", fetch free from the pool...");
                    lockService.lockPool();
                    log.info("Acquired lock for pool for lambda " + lambdaId);
                    try {
                        container = executorPool.getFreeContainer();
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
            return callLambda(container, async, jobId);

        }
    }

    private String callLambda(Container container, Boolean async, String jobId) throws Exception {

        if(!async) {
            ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:8081/execute/" + container.getLambdaId(), null, String.class);

            return response.getBody();
        }
        else {
            ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:8081/execute-async/" + container.getLambdaId() + "/" + jobId, null, String.class);
            if(response.getStatusCode() == HttpStatus.ACCEPTED) {
                // max 2h
                Long timeout = System.currentTimeMillis() + (1000 * 60 * 60 * 2);

                while(System.currentTimeMillis() < timeout) {
                    response = restTemplate.getForEntity("http://localhost:8081/execute-async/" + container.getLambdaId() + "/" + jobId, null, String.class);
                    if(response.getStatusCode() == HttpStatus.OK) {
                        return response.getBody();
                    }
                    else if(response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.debug("Not executed yet, try again");
                    }
                    else {
                        throw new Exception("Unexpected status from executor: "+response.getStatusCode());
                    }
                }

                throw new Exception("Timeout after 2h");
            }
            else {
                throw new Exception("Unable to submit the lambda for async execution");
            }
        }
    }

    public void buildInitData() throws Exception {

        int poolSize = 4;
        int nbLambda = 4;

        executorPool.initPool(poolSize);

        lambdaRepository.deleteAll();


        for(int cpt = 0; cpt < nbLambda; cpt++) {
            Lambda l = new Lambda();
            l.setId(String.valueOf(cpt+1));
            l.setContainerId(null);
            l.setLastUsedTimestamp(null);

            lambdaRepository.save(l);
            lockService.createLambdaLock(l.getId());
        }
    }
}
