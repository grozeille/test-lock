package fr.grozeille.demo.services;

import fr.grozeille.demo.model.Lambda;

public interface LambdaRepository {
    void save(Lambda lambda) throws Exception;

    Lambda load(String id) throws Exception;

    void deleteAll();
}
