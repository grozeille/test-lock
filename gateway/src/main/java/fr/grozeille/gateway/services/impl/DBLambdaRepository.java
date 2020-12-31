package fr.grozeille.gateway.services.impl;

import fr.grozeille.gateway.model.Lambda;
import fr.grozeille.gateway.services.LambdaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DBLambdaRepository implements LambdaRepository {

    @Autowired
    private JpaLambdaRepository jpaLambdaRepository;

    @Override
    public void save(Lambda lambda) throws Exception {
        jpaLambdaRepository.save(lambda);
    }

    @Override
    public Optional<Lambda> load(String id) throws Exception {
        return jpaLambdaRepository.findById(id);
    }

    @Override
    public void deleteAll() {
        jpaLambdaRepository.deleteAll();
    }
}
