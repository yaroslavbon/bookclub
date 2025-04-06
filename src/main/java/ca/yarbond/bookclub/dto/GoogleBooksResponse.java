package ca.yarbond.bookclub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Root class for mapping Google Books API responses
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksResponse {
    private int totalItems;
    private List<GoogleBookItem> items = new ArrayList<>();

    /**
     * Individual book item from Google Books API
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleBookItem {
        private String id;
        private VolumeInfo volumeInfo;
    }

    /**
     * Book volume info from Google Books API
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumeInfo {
        private String title;
        private List<String> authors;
        private String description;
        private Integer pageCount;
        private List<String> categories;
        private ImageLinks imageLinks;
        private List<IndustryIdentifier> industryIdentifiers;

        /**
         * Book cover image links
         */
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ImageLinks {
            private String smallThumbnail;
            private String thumbnail;
        }

        /**
         * Book identifiers like ISBN
         */
        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IndustryIdentifier {
            private String type;
            private String identifier;
        }
    }
}