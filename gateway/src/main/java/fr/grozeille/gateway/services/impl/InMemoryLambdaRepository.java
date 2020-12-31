package fr.grozeille.gateway.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.grozeille.gateway.model.Lambda;
import fr.grozeille.gateway.services.LambdaRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryLambdaRepository implements LambdaRepository {

    private ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, String> data = new ConcurrentHashMap<>();

    @Override
    public void save(Lambda lambda) throws Exception {
        data.put(lambda.getId(), objectMapper.writeValueAsString(lambda));
    }

    @Override
    public Optional<Lambda> load(String id) throws Exception {

        String lambda = data.get(id);
        if(lambda == null) {
            return Optional.empty();
        }

        return Optional.of(objectMapper.readValue(lambda, Lambda.class));
    }

    @Override
    public void deleteAll() {
        data.clear();
    }
}
