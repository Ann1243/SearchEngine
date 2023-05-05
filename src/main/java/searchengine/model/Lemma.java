package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "lemma")
public class Lemma implements Comparable<Lemma>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "lemma")
    private List<Index> indexes;
    @Override
    public int compareTo(Lemma o) {
        return this.frequency.compareTo(o.frequency); }
}
