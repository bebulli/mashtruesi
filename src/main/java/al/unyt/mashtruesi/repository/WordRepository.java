package al.unyt.mashtruesi.repository;

import al.unyt.mashtruesi.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {
    List<Word> findByCategory_NameIgnoreCase(String categoryName);
}
