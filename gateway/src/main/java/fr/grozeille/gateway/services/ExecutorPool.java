package fr.grozeille.gateway.services;

import fr.grozeille.gateway.model.Container;

public interface ExecutorPool {
    Container getFreeContainer() throws Exception;

    void updateContainer(Container c) throws Exception;

    Container getContainer(String containerId) throws Exception;

    Container getContainerByLambdaId(String lambdaId) throws Exception;

    Container assignFirstFree(String lambdaId) throws Exception;

    void initPool(int poolSize) throws Exception;
}
