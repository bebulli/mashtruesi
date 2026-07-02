package al.unyt.mashtruesi.web;

import al.unyt.mashtruesi.model.Word;
import al.unyt.mashtruesi.service.WordService;
import al.unyt.mashtruesi.web.dto.AddWordRequest;
import al.unyt.mashtruesi.web.dto.CategoryDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class CategoryController {
    private final WordService wordService;

    public CategoryController(WordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping("/api/categories")
    public List<CategoryDto> categories() {
        Map<String, Long> counts = wordService.wordCountsByCategory();
        return wordService.listCategoryNames().stream()
                .map(name -> new CategoryDto(name, counts.getOrDefault(name, 0L)))
                .toList();
    }

    @PostMapping("/api/words")
    public ResponseEntity<CategoryDto> addWord(@RequestBody AddWordRequest request) {
        Word saved = wordService.addWord(request.category(), request.text(), request.hint());
        String categoryName = saved.getCategory().getName();
        Map<String, Long> counts = wordService.wordCountsByCategory();
        CategoryDto body = new CategoryDto(categoryName, counts.getOrDefault(categoryName, 0L));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
