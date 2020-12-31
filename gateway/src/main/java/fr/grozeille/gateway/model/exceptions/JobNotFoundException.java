package fr.grozeille.gateway.model.exceptions;

public class JobNotFoundException extends Exception {
    public JobNotFoundException(String jobId) {
        super("Job "+jobId+" doesn't exist");
    }
}
