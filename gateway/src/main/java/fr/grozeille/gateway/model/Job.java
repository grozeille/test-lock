package fr.grozeille.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Job {

    public enum JobStatus {
        SUBMITTED,
        RUNNING,
        ENDED
    }

    @Id
    @Column(name = "id")
    private String id;
    @Column(name = "startDateTimestamp")
    private Long startDateTimestamp;
    @Column(name = "endDateTimestamp")
    private Long endDateTimestamp;
    @Column(name = "submitDateTimestamp")
    private Long submitDateTimestamp;
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    @Column(name = "lambdaId")
    private String lambdaId;
    @Column(name = "result")
    private String result;

    @JsonIgnore
    @Column(name = "tokenHash")
    private String tokenHash;

}
