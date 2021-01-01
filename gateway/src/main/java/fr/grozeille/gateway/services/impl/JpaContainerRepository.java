package fr.grozeille.gateway.services.impl;

import fr.grozeille.gateway.model.Container;
import fr.grozeille.gateway.model.Job;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaContainerRepository extends CrudRepository<Container, String> {

    @Deprecated
    @Query(value="SELECT * FROM container WHERE lambdaId = 'free' LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Container findFirstFree();

    @Query(value = "UPDATE container SET lambdaId = ?1\n" +
            "WHERE containerid = (\n" +
            "    SELECT containerid\n" +
            "    FROM container\n" +
            "    WHERE lambdaId = 'free'\n" +
            "    FOR UPDATE SKIP LOCKED\n" +
            "    LIMIT 1\n" +
            ")\n" +
            "RETURNING *;", nativeQuery = true)
    Container assignFirstFree(String lambdaId);

    @Query(value="SELECT * FROM container WHERE lambdaId = ?1 LIMIT 1 FOR UPDATE", nativeQuery = true)
    Container findByLambdaId(String lambdaId);
}
