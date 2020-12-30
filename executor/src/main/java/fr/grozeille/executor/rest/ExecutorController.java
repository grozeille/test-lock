package fr.grozeille.executor.rest;

import fr.grozeille.executor.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class ExecutorController {

    private final ArrayBlockingQueue<Job> toExecute = new ArrayBlockingQueue<>(10);
    private final ArrayBlockingQueue<Job> executed = new ArrayBlockingQueue<>(10);

    @PostConstruct
    public void init() {
        Thread thread = new Thread(() -> {
            log.info("Async lambda call thread executor started...");
            while(true) {
                try {
                    Job job = toExecute.poll(1, TimeUnit.SECONDS);
                    if(job != null) {
                        log.info("Calling async Lambda " + job.getLambdaId());
                        job.setResult(this.internalExecute(job.getLambdaId()));
                        executed.add(job);
                    }
                } catch (InterruptedException ignoredException) {
                }
            }
        });
        thread.setName("AsyncExecutor");
        thread.start();
    }

    private String internalExecute(String id) throws InterruptedException {
        // simulate time to execute python
        Thread.sleep(1000*10);
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

        Job job = executed.poll(timeout, TimeUnit.MILLISECONDS);
        if(job != null) {
            return ResponseEntity.ok(job.getResult());
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }
}
