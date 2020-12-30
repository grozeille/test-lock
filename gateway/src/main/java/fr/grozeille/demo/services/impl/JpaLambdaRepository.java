package fr.grozeille.demo.services.impl;

import fr.grozeille.demo.model.Lambda;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaLambdaRepository extends CrudRepository<Lambda, String> {
}
