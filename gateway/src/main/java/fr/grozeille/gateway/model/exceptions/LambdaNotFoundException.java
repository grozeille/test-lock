package fr.grozeille.gateway.model.exceptions;

public class LambdaNotFoundException extends Exception {
    public LambdaNotFoundException(String lambdaId) {
        super("Lambda "+lambdaId+" doesn't exist");
    }
}
