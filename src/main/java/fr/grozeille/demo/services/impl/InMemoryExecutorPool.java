package fr.grozeille.demo.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.grozeille.demo.model.Container;
import fr.grozeille.demo.services.ExecutorPool;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryExecutorPool implements ExecutorPool {

    private ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, String> containers = new ConcurrentHashMap<>();

    @Override
    public Container getFreeContainer() throws Exception {
        for(String s : containers.values()) {
            Container c = objectMapper.readValue(s, Container.class);
            if(c.getLambdaId().equals("free")) {
                return c;
            }
        }
        return null;
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
    public void initPool(int poolSize) throws Exception {
        for(int cpt = 0; cpt < poolSize; cpt++) {
            Container c = new Container();
            c.setLambdaId("free");
            c.setContainerId(UUID.randomUUID().toString());
            containers.put(c.getContainerId(), objectMapper.writeValueAsString(c));
        }
    }
}
