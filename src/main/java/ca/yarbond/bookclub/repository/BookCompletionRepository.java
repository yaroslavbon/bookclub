package ca.yarbond.bookclub.repository;

import ca.yarbond.bookclub.model.BookCompletionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCompletionRepository extends JpaRepository<BookCompletionRecord, BookCompletionRecord.BookCompletionId> {
    
    List<BookCompletionRecord> findByBookId(Long bookId);
    
    List<BookCompletionRecord> findByMemberId(Long memberId);
    
    @Query("SELECT COUNT(r) FROM BookCompletionRecord r WHERE r.book.id = :bookId")
    int countCompletionsForBook(@Param("bookId") Long bookId);
    
    boolean existsByBookIdAndMemberId(Long bookId, Long memberId);
}