package ca.yarbond.bookclub.controller;

import ca.yarbond.bookclub.dto.BookSearchDto;
import ca.yarbond.bookclub.service.GoogleBooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for book search functionality
 */
@RestController
@RequestMapping("/api/book-search")
public class BookSearchController {

    private final GoogleBooksService googleBooksService;

    @Autowired
    public BookSearchController(GoogleBooksService googleBooksService) {
        this.googleBooksService = googleBooksService;
    }

    /**
     * Search for books using Google Books API
     *
     * @param query    The search query text
     * @param language The preferred language code, defaults to "ru"
     * @return List of book search results
     */
    @GetMapping
    public ResponseEntity<List<BookSearchDto>> searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "ru") String language) {

        List<BookSearchDto> results = googleBooksService.searchBooks(query, language);
        return ResponseEntity.ok(results);
    }
}