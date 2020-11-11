package fr.grozeille.demo.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Container {
    @Id
    @Column(name = "containerId")
    private String containerId;
    @Column(name = "lambdaId")
    private String lambdaId;

    public void callLambda() throws InterruptedException {
        Thread.sleep(200);
        System.out.println("Coucou " + lambdaId);
    }
}
