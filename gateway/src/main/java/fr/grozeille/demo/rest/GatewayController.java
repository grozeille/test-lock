package fr.grozeille.demo.rest;

import fr.grozeille.demo.model.Job;
import fr.grozeille.demo.services.impl.ExecutorService;
import fr.grozeille.demo.services.impl.JpaJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class GatewayController {

    private final ExecutorService executorService;

    private final JpaJobRepository jobRepository;

    public GatewayController(ExecutorService executorService, JpaJobRepository jobRepository) {
        this.executorService = executorService;
        this.jobRepository = jobRepository;
    }

    @PostConstruct
    public void init() throws Exception {

        this.executorService.buildInitData();

        for(int cpt = 0; cpt < 10; cpt++){
            final int cptFinal = cpt;
            Thread thread = new Thread(() -> {
                log.info("Async lambda call thread "+cptFinal+" started...");

                while(true) {

                    Optional<Job> job = Optional.empty();
                    job = jobRepository.poll(System.currentTimeMillis());
                    if(job.isPresent()) {

                        try {
                            String result = executorService.callLambdaAsync(job.get().getLambdaId(), job.get().getId());
                            job.get().setResult(result);
                        } catch (Exception e) {
                            log.error("Unable to execute Lambda " + job.get().getLambdaId(), e);
                        }
                        finally {
                            job.get().setEndDateTimestamp(System.currentTimeMillis());
                            job.get().setStatus(Job.JobStatus.ENDED);
                            jobRepository.save(job.get());
                        }
                    }
                    else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            log.error("Error while waiting for next poll", e);
                        }
                    }
                }
            });
            thread.setName("JobThread-"+cpt);
            thread.start();
        }
    }

    @PostMapping("/execute/{id}")
    public String execute(@PathVariable String id) throws Exception {
        log.info("Calling Lambda " + id);
        Thread.sleep(1000*10000);
        return this.executorService.callLambda(id);
    }

    @PostMapping("/execute-async/{id}")
    public ResponseEntity<String> executeAsync(@PathVariable String id) throws Exception {
        log.info("Calling Lambda " + id + " async");

        String requestId = UUID.randomUUID().toString();

        UriComponents rootUri = ServletUriComponentsBuilder.fromCurrentContextPath().build();

        // create a new job for the execution
        Job job = new Job();
        job.setSubmitDateTimestamp(System.currentTimeMillis());
        job.setStatus(Job.JobStatus.SUBMITTED);
        job.setId(requestId);
        job.setLambdaId(id);

        this.jobRepository.save(job);

        // return the url of the job
        return ResponseEntity.accepted().location(URI.create(rootUri.toUriString()+ "/get-result/"+requestId)).body(requestId);
    }

    @GetMapping("/get-result/{id}")
    public ResponseEntity<Job> getJobResult(@PathVariable String id) throws Exception {
        Optional<Job> jobOptional = this.jobRepository.findById(id);

        if(jobOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(jobOptional.get());
    }
}
