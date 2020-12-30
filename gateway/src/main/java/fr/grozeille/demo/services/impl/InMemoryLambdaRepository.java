package fr.grozeille.demo.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.grozeille.demo.model.Lambda;
import fr.grozeille.demo.services.LambdaRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
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
    public Lambda load(String id) throws Exception {
        return objectMapper.readValue(data.get(id), Lambda.class);
    }

    @Override
    public void deleteAll() {
        data.clear();
    }
}
