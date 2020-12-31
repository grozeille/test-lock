package fr.grozeille.gateway.services.impl;

import fr.grozeille.gateway.services.LockService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class InMemoryLockService implements LockService {
    private Map<String, ReentrantLock> lambdaLocks = new ConcurrentHashMap<>();

    private ReentrantLock poolLock = new ReentrantLock();

    @Override
    public void createLambdaLock(String lambdaId) {
        lambdaLocks.put(lambdaId, new ReentrantLock());
    }

    @Override
    public void lockLambda(String lambdaId) {
        lambdaLocks.get(lambdaId).lock();
    }

    @Override
    public void unlockLambda(String lambdaId) {
        lambdaLocks.get(lambdaId).unlock();
    }

    @Override
    public void lockPool() {
        poolLock.lock();
    }

    @Override
    public void unlockPool() {
        poolLock.unlock();
    }

}
