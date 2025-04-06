package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.dto.BookSearchDto;
import ca.yarbond.bookclub.dto.GoogleBooksResponse;
import ca.yarbond.bookclub.dto.GoogleBooksResponse.GoogleBookItem;
import ca.yarbond.bookclub.dto.GoogleBooksResponse.VolumeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GoogleBooksService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleBooksService.class);
    private static final String API_BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    @Value("${google.books.api.key:}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, List<BookSearchDto>> searchCache = new ConcurrentHashMap<>();

    // Cache expiration in milliseconds (1 hour)
    private static final long CACHE_EXPIRATION = 3600000L;
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();


    public GoogleBooksService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Search for books using Google Books API
     *
     * @param query    The search query text
     * @param language The preferred language code (e.g., "ru", "uk", "en")
     * @return List of book search results
     */
    public List<BookSearchDto> searchBooks(String query, String language) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Clean up expired cache entries
        cleanExpiredCache();

        // Check cache first
        String cacheKey = query.toLowerCase() + "_" + language;
        if (searchCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for query: {}", query);
            return searchCache.get(cacheKey);
        }


        try {
            // Build search URL with language preference
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(API_BASE_URL)
                    .queryParam("q", query)
                    .queryParam("maxResults", 10);

            if (language != null && !language.trim().isEmpty()) {
                builder.queryParam("langRestrict", language);
            }

            if (apiKey != null && !apiKey.trim().isEmpty()) {
                builder.queryParam("key", apiKey);
            }

            String url = builder.build().encode().toUriString();
            logger.debug("Searching books with URL: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Error response from Google Books API: {} {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

            GoogleBooksResponse booksResponse = objectMapper.readValue(response.body(), GoogleBooksResponse.class);
            List<BookSearchDto> results = mapToBookDtos(booksResponse);

            // Cache results
            searchCache.put(cacheKey, results);
            cacheTimestamps.put(cacheKey, System.currentTimeMillis());

            return results;
        } catch (IOException | InterruptedException e) {
            logger.error("Error searching books: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Map Google Books API response to our DTOs
     */
    private List<BookSearchDto> mapToBookDtos(GoogleBooksResponse response) {
        if (response == null || response.getItems() == null) {
            return Collections.emptyList();
        }

        return response.getItems().stream()
                .map(this::mapBookItemToDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Map a single Google book item to our DTO
     */
    private BookSearchDto mapBookItemToDto(GoogleBookItem item) {
        if (item == null || item.getVolumeInfo() == null) {
            return null;
        }

        VolumeInfo info = item.getVolumeInfo();

        // Extract authors
        String author = null;
        if (info.getAuthors() != null && !info.getAuthors().isEmpty()) {
            author = String.join(", ", info.getAuthors());
        }

        // Extract ISBN
        String isbn = null;
        if (info.getIndustryIdentifiers() != null) {
            for (VolumeInfo.IndustryIdentifier identifier : info.getIndustryIdentifiers()) {
                if ("ISBN_13".equals(identifier.getType())) {
                    isbn = identifier.getIdentifier();
                    break;
                } else if ("ISBN_10".equals(identifier.getType()) && isbn == null) {
                    isbn = identifier.getIdentifier();
                }
            }
        }

        // Extract cover URL
        String coverUrl = null;
        if (info.getImageLinks() != null) {
            coverUrl = info.getImageLinks().getThumbnail();
        }

        // Determine if fiction
        boolean fiction = false;
        if (info.getCategories() != null) {
            fiction = info.getCategories().stream()
                    .anyMatch(category -> category.toLowerCase().contains("fiction") ||
                            category.toLowerCase().contains("novel") ||
                            category.toLowerCase().contains("роман") ||
                            category.toLowerCase().contains("художественная"));
        }

        return BookSearchDto.builder()
                .title(info.getTitle())
                .author(author)
                .description(info.getDescription())
                .pageCount(info.getPageCount())
                .isbn(isbn)
                .coverUrl(coverUrl)
                .fiction(fiction)
                .build();
    }

    /**
     * Clean expired cache entries
     */
    private void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        List<String> expiredKeys = cacheTimestamps.entrySet().stream()
                .filter(entry -> now - entry.getValue() > CACHE_EXPIRATION)
                .map(Map.Entry::getKey)
                .toList();

        for (String key : expiredKeys) {
            searchCache.remove(key);
            cacheTimestamps.remove(key);
        }
    }
}