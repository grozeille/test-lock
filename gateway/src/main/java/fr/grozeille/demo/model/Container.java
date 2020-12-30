package fr.grozeille.demo.model;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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
