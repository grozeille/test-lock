package fr.grozeille.executor.rest;

import fr.grozeille.executor.ExecutorConfig;
import fr.grozeille.executor.model.ExecuteRequest;
import fr.grozeille.executor.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class ExecutorController {

    private ArrayBlockingQueue<Job> toExecute;
    private Map<String, Job> executed = new ConcurrentHashMap<>();
    private static final int MAX_CALLBACK_TRY = 3;

    @Autowired
    private ExecutorConfig executorConfig;

    @PostConstruct
    public void init() {
        toExecute = new ArrayBlockingQueue<>(executorConfig.getAsyncMaxQueue());

        Thread threadExecutor = new Thread(() -> {
            log.info("Async lambda call thread executor started...");
            while(true) {
                try {
                    Job job = toExecute.poll(1, TimeUnit.SECONDS);
                    if(job != null) {
                        log.info("Calling async Lambda " + job.getLambdaId());
                        job.setResult(this.internalExecute(job.getLambdaId()));
                        executed.put(job.getLambdaId()+"#"+job.getId(), job);
                    }
                } catch (InterruptedException ignoredException) {
                }
            }
        });
        threadExecutor.setName("AsyncExecutor");
        threadExecutor.start();

        Thread threadCallback = new Thread(() -> {
            log.info("Async lambda callback thread started...");
            RestTemplate restTemplate = new RestTemplate();
            while(true) {
                // get all keys from the executed map
                for(Map.Entry<String, Job> kv : executed.entrySet()) {
                    // if a callback is set, try to call it
                    if(kv.getValue().getCallbackUrl() != null) {
                        if(kv.getValue().getCallbackTry() < MAX_CALLBACK_TRY) {
                            kv.getValue().setCallbackTry(kv.getValue().getCallbackTry()+1);
                            try {
                                ResponseEntity<String> response = restTemplate.getForEntity(kv.getValue().getCallbackUrl(), String.class);
                                if(response.getStatusCodeValue() >= 400) {
                                    throw new Exception("Unexpected status code: "+response.getStatusCodeValue()+" with body: "+response.getBody());
                                }
                            }
                            catch(Exception callbackEx) {
                                log.warn("Unable to call the callback URL for Lambda "+kv.getValue().getLambdaId()+": "+callbackEx.toString());
                                kv.getValue().getCallbackErrors().add(callbackEx);
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignoredException) {
                }
            }
        });
        threadCallback.setName("AsyncCallback");
        threadCallback.start();
    }

    private String internalExecute(String id) throws InterruptedException {
        // simulate time to execute python
        log.info("Simulating execution time: "+executorConfig.getExecutionTimeMs());
        Thread.sleep(executorConfig.getExecutionTimeMs());
        return "Hello from "+id;
    }


    @PostMapping("/execute/{id}")
    public String execute(@PathVariable String id) throws InterruptedException {
      log.info("Calling Lambda " + id);
      return this.internalExecute(id);
    }

    @PostMapping("/execute-async/{lambdaId}/{jobId}")
    public ResponseEntity<?> executeAsync(@PathVariable String lambdaId, @PathVariable String jobId) throws InterruptedException {
        log.info("Submitting async call for Lambda  " + lambdaId);

        Job job = new Job();
        job.setId(jobId);
        job.setLambdaId(lambdaId);
        toExecute.add(job);


        UriComponents rootUri = ServletUriComponentsBuilder.fromCurrentContextPath().build();
        return ResponseEntity.accepted().location(URI.create(rootUri.toUriString()+ "/execute-async/"+lambdaId+"/"+jobId)).build();
    }

    @GetMapping("/execute-async/{lambdaId}/{jobId}")
    public ResponseEntity<String> getResult(
            @PathVariable String lambdaId,
            @PathVariable String jobId,
            @RequestParam(required = false, defaultValue = "10000") Integer timeout) throws InterruptedException {

        Job job = null;
        long maxTime = System.currentTimeMillis()+timeout;
        do {
            job = executed.remove(lambdaId+"#"+jobId);
            if(job == null) {
                Thread.sleep(100);
            }
        } while(job == null && System.currentTimeMillis() < maxTime);

        if(job == null) {
            return ResponseEntity.notFound().build();
        }
        else {
            return ResponseEntity.ok(job.getResult());
        }
    }

    @PostMapping("/execute-async-callback/{lambdaId}/{jobId}")
    public ResponseEntity<?> executeAsyncCallback(@PathVariable String lambdaId, @PathVariable String jobId, @RequestBody ExecuteRequest executeRequest) throws InterruptedException {
        log.info("Submitting async call with callback for Lambda  " + lambdaId);

        Job job = new Job();
        job.setId(jobId);
        job.setLambdaId(lambdaId);
        job.setCallbackUrl(executeRequest.getCallbackUrl());

        toExecute.add(job);

        return ResponseEntity.accepted().build();
    }
}
