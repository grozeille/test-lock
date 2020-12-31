package fr.grozeille.gateway.services.impl;

import fr.grozeille.gateway.model.Lambda;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaLambdaRepository extends CrudRepository<Lambda, String> {
}
