package fr.grozeille.executor.model;

import lombok.Data;

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
}
