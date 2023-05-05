package searchengine.model;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "`index`")
public class IndexModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private Page page;
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    private Lemma lemma;
    @Column(name = "lemma_rank", nullable = false)
    private float lemmaRank;
}
