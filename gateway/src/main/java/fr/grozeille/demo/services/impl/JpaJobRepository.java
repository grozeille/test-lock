package fr.grozeille.demo.services.impl;

import fr.grozeille.demo.model.Job;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaJobRepository extends CrudRepository<Job, String> {

    @Query(value = "UPDATE job SET status = 'RUNNING', startdatetimestamp = ?1\n" +
            "WHERE id = (\n" +
            "    SELECT id\n" +
            "    FROM job\n" +
            "    WHERE status = 'SUBMITTED'\n" +
            "    ORDER BY submitdatetimestamp\n" +
            "    FOR UPDATE SKIP LOCKED\n" +
            "    LIMIT 1\n" +
            ")\n" +
            "RETURNING *;", nativeQuery = true)
    Optional<Job> poll(long startTime);
}
