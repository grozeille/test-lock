package fr.grozeille.demo.services.impl;

import fr.grozeille.demo.model.Lambda;
import fr.grozeille.demo.services.LambdaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DBLambdaRepository implements LambdaRepository {

    @Autowired
    private JpaLambdaRepository jpaLambdaRepository;

    @Override
    public void save(Lambda lambda) throws Exception {
        jpaLambdaRepository.save(lambda);
    }

    @Override
    public Lambda load(String id) throws Exception {
        return jpaLambdaRepository.findById(id).get();
    }

    @Override
    public void deleteAll() {
        jpaLambdaRepository.deleteAll();
    }
}
