package fr.grozeille.gateway.model.exceptions;

public class TokenMismatchException extends Exception {
    public TokenMismatchException(String jobId) {
        super("Mismatch of token for Job "+jobId);
    }
}
