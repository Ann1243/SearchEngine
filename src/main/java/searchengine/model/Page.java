package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
@Getter
@Setter
@Entity
@Table(name = "page", indexes = {@Index(name = "path_index", columnList = "path")})
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "path", nullable = false, unique = true, columnDefinition = "VARCHAR(256)")
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "page")
    private List<Index> indexes ;
}
