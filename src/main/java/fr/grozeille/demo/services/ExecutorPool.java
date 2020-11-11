package fr.grozeille.demo.services;

import fr.grozeille.demo.model.Container;

public interface ExecutorPool {
    Container getFreeContainer() throws Exception;

    void updateContainer(Container c) throws Exception;

    Container getContainer(String c) throws Exception;

    void initPool(int poolSize) throws Exception;
}
