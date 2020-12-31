package fr.grozeille.gateway.model;

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
}
