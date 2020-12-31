package fr.grozeille.executor.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Job {
    public enum JobStatus {
        SUBMITTED,
        RUNNING,
        ENDED
    }
    private String id;
    private String lambdaId;
    private JobStatus status;
    private String result;
    private String callbackUrl;
    private int callbackTry = 0;
    private List<Exception> callbackErrors = new ArrayList<>();
}
