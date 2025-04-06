package ca.yarbond.bookclub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for book search results from external APIs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchDto {
    private String title;
    private String author;
    private String description;
    private Integer pageCount;
    private String isbn;
    private String coverUrl;
    private boolean fiction;
}