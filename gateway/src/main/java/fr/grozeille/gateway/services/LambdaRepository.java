package fr.grozeille.gateway.services;

import fr.grozeille.gateway.model.Lambda;

import java.util.Optional;

public interface LambdaRepository {
    void save(Lambda lambda) throws Exception;

    Optional<Lambda> load(String id) throws Exception;

    void deleteAll();
}
