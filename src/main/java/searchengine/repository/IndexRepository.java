package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexModel;
import searchengine.model.Lemma;
import searchengine.model.Page;

@Repository
public interface IndexRepository extends JpaRepository<IndexModel, Integer> {
    IndexModel findByLemmaAndPage(Lemma lemma, Page page);
}
