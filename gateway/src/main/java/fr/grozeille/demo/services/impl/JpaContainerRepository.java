package fr.grozeille.demo.services.impl;

import fr.grozeille.demo.model.Container;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;

@Repository
public interface JpaContainerRepository extends CrudRepository<Container, String> {

    @Query(value="SELECT * FROM container WHERE lambdaId = 'free' LIMIT 1", nativeQuery = true)
    Container findFirstFree();
}
