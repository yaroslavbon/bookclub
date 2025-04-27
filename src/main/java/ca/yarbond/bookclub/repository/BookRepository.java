package ca.yarbond.bookclub.repository;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookStatus;
import ca.yarbond.bookclub.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Non-paginated methods
    List<Book> findByStatus(BookStatus status);
    List<Book> findByOwner(Member owner);
    List<Book> findByOwnerId(Long ownerId);
    List<Book> findByFiction(boolean isFiction);
    List<Book> findByOwnerAndStatus(Member owner, BookStatus status);
    List<Book> findByOwnerIdAndStatus(Long ownerId, BookStatus status);

    @Query("SELECT b FROM Book b WHERE b.status = 'COMPLETED' ORDER BY b.completionDate DESC")
    List<Book> findRecentlyCompletedBooks();

    @Query(value = "SELECT * FROM books WHERE status = 'COMPLETED' ORDER BY completion_date DESC LIMIT 5",
            nativeQuery = true)
    List<Book> findTop5RecentlyCompletedBooks();

    @Query("SELECT b FROM Book b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.comments) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.owner.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Book> searchBooks(@Param("searchTerm") String searchTerm);
    
    // Paginated methods
    Page<Book> findAll(Pageable pageable);
    
    Page<Book> findByStatus(BookStatus status, Pageable pageable);
    
    Page<Book> findByOwnerId(Long ownerId, Pageable pageable);
    
    Page<Book> findByOwnerIdAndStatus(Long ownerId, BookStatus status, Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE " +
            "LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.comments) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.owner.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Book> searchBooks(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Enhanced search methods with combined filters
    @Query("SELECT b FROM Book b WHERE " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.comments) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.owner.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND b.status = :status")
    Page<Book> searchBooksByStatus(
            @Param("searchTerm") String searchTerm, 
            @Param("status") BookStatus status, 
            Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.comments) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.owner.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND b.owner.id = :ownerId")
    Page<Book> searchBooksByOwnerId(
            @Param("searchTerm") String searchTerm, 
            @Param("ownerId") Long ownerId, 
            Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.comments) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.owner.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND b.status = :status " +
            "AND b.owner.id = :ownerId")
    Page<Book> searchBooksByStatusAndOwnerId(
            @Param("searchTerm") String searchTerm, 
            @Param("status") BookStatus status, 
            @Param("ownerId") Long ownerId, 
            Pageable pageable);
}