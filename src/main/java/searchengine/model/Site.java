package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import searchengine.config.Status;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.ORDINAL)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private Date statusTime;

    @Column(name = "error")
    private String error;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "name", nullable = false)
    private String name;
}
