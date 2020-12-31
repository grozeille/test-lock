package fr.grozeille.gateway.services.impl;

import fr.grozeille.gateway.model.Container;
import fr.grozeille.gateway.services.ExecutorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DBExecutorPool implements ExecutorPool {

    @Autowired
    private JpaContainerRepository jpaContainerRepository;

    @Override
    public Container getFreeContainer() throws Exception {
        return jpaContainerRepository.findFirstFree();
    }

    @Override
    public void updateContainer(Container c) throws Exception {
        jpaContainerRepository.save(c);
    }

    @Override
    public Container getContainer(String c) throws Exception {
        return jpaContainerRepository.findById(c).get();
    }

    @Override
    public void initPool(int poolSize) throws Exception {
        jpaContainerRepository.deleteAll();
        for(int cpt = 0; cpt < poolSize; cpt++) {
            Container c = new Container();
            c.setLambdaId("free");
            c.setContainerId(UUID.randomUUID().toString());
            jpaContainerRepository.save(c);
        }
    }
}
