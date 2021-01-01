package fr.grozeille.gateway.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.grozeille.gateway.model.Container;
import fr.grozeille.gateway.services.ExecutorPool;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryExecutorPool implements ExecutorPool {

    private ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, String> containers = new ConcurrentHashMap<>();

    private final Object lock = new Object();

    @Override
    public Container getFreeContainer() throws Exception {
        return getContainerByLambdaId("free");
    }

    @Override
    public void updateContainer(Container c) throws Exception {
        containers.put(c.getContainerId(), objectMapper.writeValueAsString(c));
    }

    @Override
    public Container getContainer(String c) throws Exception {
        return objectMapper.readValue(containers.get(c), Container.class);
    }

    @Override
    public Container getContainerByLambdaId(String lambdaId) throws Exception {
        for(String s : containers.values()) {
            Container c = objectMapper.readValue(s, Container.class);
            if(c.getLambdaId().equals(lambdaId)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Container assignFirstFree(String lambdaId) throws Exception {
        synchronized (this.lock) {
            Container c = this.getContainerByLambdaId("free");
            c.setLambdaId(lambdaId);
            this.updateContainer(c);

            return c;
        }
    }

    @Override
    public void initPool(int poolSize) throws Exception {
        for(int cpt = 0; cpt < poolSize; cpt++) {
            Container c = new Container();
            c.setLambdaId("free");
            c.setContainerId(UUID.randomUUID().toString());
            containers.put(c.getContainerId(), objectMapper.writeValueAsString(c));
        }
    }
}
