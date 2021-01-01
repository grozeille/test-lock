package fr.grozeille.gateway.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Lambda {
    @Id
    private String id;
}
