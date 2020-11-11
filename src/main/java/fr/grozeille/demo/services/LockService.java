package fr.grozeille.demo.services;

public interface LockService {
    void createLambdaLock(String lambdaId);

    void lockLambda(String lambdaId);

    void unlockLambda(String lambdaId);

    void lockPool();

    void unlockPool();
}
