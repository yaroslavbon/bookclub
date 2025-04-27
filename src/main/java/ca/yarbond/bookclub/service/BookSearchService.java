package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * Service class to encapsulate book search and filtering logic
 */
@Service
public class BookSearchService {

    private final BookService bookService;
    public static final int DEFAULT_PAGE_SIZE = 10;

    @Autowired
    public BookSearchService(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Search for books with flexible filter combinations and pagination
     *
     * @param search Search term for title/author/comments (optional)
     * @param status Book status filter (optional)
     * @param ownerId Owner ID filter (optional)
     * @param page Page number (0-based)
     * @return Page of books matching the search criteria
     */
    public Page<Book> searchBooks(String search, String status, Long ownerId, int page) {
        // Convert status string to enum if present
        BookStatus bookStatus = BookStatus.fromString(status);

        // Check for empty strings and null values
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = bookStatus != null;
        boolean hasOwner = ownerId != null;
        
        // Handle different filter combinations using the most optimized repository method
        if (hasSearch) {
            if (hasStatus && hasOwner) {
                // Use optimized method for combined search + status + owner filter
                return bookService.searchBooksByStatusAndOwnerId(search, bookStatus, ownerId, page, DEFAULT_PAGE_SIZE);
            } else if (hasStatus) {
                // Search with status filter
                return bookService.searchBooksByStatus(search, bookStatus, page, DEFAULT_PAGE_SIZE);
            } else if (hasOwner) {
                // Search with owner filter
                return bookService.searchBooksByOwnerId(search, ownerId, page, DEFAULT_PAGE_SIZE);
            } else {
                // Just search
                return bookService.searchBooks(search, page, DEFAULT_PAGE_SIZE);
            }
        } else if (hasStatus && hasOwner) {
            // Filter by both status and owner
            return bookService.getBooksByOwnerIdAndStatus(ownerId, bookStatus, page, DEFAULT_PAGE_SIZE);
        } else if (hasStatus) {
            // Filter by status only
            return bookService.getBooksByStatus(bookStatus, page, DEFAULT_PAGE_SIZE);
        } else if (hasOwner) {
            // Filter by owner only
            return bookService.getBooksByOwnerId(ownerId, page, DEFAULT_PAGE_SIZE);
        } else {
            // No filters, get all books
            return bookService.getAllBooks(page, DEFAULT_PAGE_SIZE);
        }
    }
}