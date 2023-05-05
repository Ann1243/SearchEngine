package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Field;

@Repository
public interface FieldRepository extends JpaRepository<Field, Integer> {
    Field findByName(String name);
}
