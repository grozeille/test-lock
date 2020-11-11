package fr.grozeille.demo.services.impl;

import fr.grozeille.demo.model.Container;
import fr.grozeille.demo.model.Lambda;
import fr.grozeille.demo.services.ExecutorPool;
import fr.grozeille.demo.services.LambdaRepository;
import fr.grozeille.demo.services.LockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

@Service
@Slf4j
public class MyService {

    @Autowired
    private DBLockService lockService;

    @Autowired
    private DBLambdaRepository lambdaRepository;

    @Autowired
    private DBExecutorPool executorPool;

    @Transactional(readOnly = false)
    public void callLambda(String lambdaId) throws Exception {

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
            container.callLambda();
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
            container.callLambda();

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
