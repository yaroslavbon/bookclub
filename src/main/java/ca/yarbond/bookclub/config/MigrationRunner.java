package ca.yarbond.bookclub.config;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookStatus;
import ca.yarbond.bookclub.service.BookCompletionService;
import ca.yarbond.bookclub.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs after data initializers to perform data migrations for existing data
 */
@Component
@Order(100) // Run after other initializers
public class MigrationRunner implements CommandLineRunner {

    private final BookService bookService;
    private final BookCompletionService bookCompletionService;

    @Autowired
    public MigrationRunner(BookService bookService, BookCompletionService bookCompletionService) {
        this.bookService = bookService;
        this.bookCompletionService = bookCompletionService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only run migration if specifically requested with --run-migration argument
        boolean shouldRunMigration = false;
        for (String arg : args) {
            if ("--run-migration".equals(arg)) {
                shouldRunMigration = true;
                break;
            }
        }
        
        if (!shouldRunMigration) {
            return;
        }
        
        // Mark completed books as read by all members
        List<Book> completedBooks = bookService.getBooksByStatus(BookStatus.COMPLETED);
        for (Book book : completedBooks) {
            bookCompletionService.markCompletedBookAsReadByAllMembers(book.getId());
        }
    }
}