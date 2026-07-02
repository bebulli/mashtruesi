package al.unyt.mashtruesi.service;

import al.unyt.mashtruesi.exception.InvalidSettingsException;
import al.unyt.mashtruesi.model.Category;
import al.unyt.mashtruesi.model.Word;
import al.unyt.mashtruesi.repository.CategoryRepository;
import al.unyt.mashtruesi.repository.WordRepository;
import al.unyt.mashtruesi.util.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WordService {
    private final CategoryRepository categoryRepository;
    private final WordRepository wordRepository;

    public WordService(CategoryRepository categoryRepository, WordRepository wordRepository) {
        this.categoryRepository = categoryRepository;
        this.wordRepository = wordRepository;
    }

    @Transactional(readOnly = true)
    public List<String> listCategoryNames() {
        List<String> names = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());
        return CollectionUtils.sortedDistinct(names);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> wordCountsByCategory() {
        return wordRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        w -> w.getCategory().getName(),
                        Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public Word pickRandomWord(String categoryName, Random random) {
        List<Word> words = wordRepository.findByCategory_NameIgnoreCase(categoryName);
        if (words.isEmpty()) {
            throw new InvalidSettingsException(
                    "Kategoria '" + categoryName + "' nuk ka fjale (no words available).");
        }
        return words.get(random.nextInt(words.size()));
    }

    @Transactional
    public Word addWord(String categoryName, String text, String hint) {
        if (text == null || text.isBlank()) {
            throw new InvalidSettingsException("Fjala nuk mund te jete bosh.");
        }
        Function<String, Category> createCategory = name -> categoryRepository.save(new Category(name));
        Category category = categoryRepository.findByNameIgnoreCase(categoryName)
                .orElseGet(() -> createCategory.apply(categoryName));
        Word word = category.addWord(text.trim(), hint);
        categoryRepository.save(category);
        return word;
    }
}
