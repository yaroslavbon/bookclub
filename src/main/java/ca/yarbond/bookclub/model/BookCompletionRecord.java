package ca.yarbond.bookclub.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "book_completion_records")
@Data
@NoArgsConstructor
public class BookCompletionRecord {

    @EmbeddedId
    private BookCompletionId id;
    
    @ManyToOne
    @MapsId("bookId")
    @JoinColumn(name = "book_id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Book book;
    
    @ManyToOne
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Member member;
    
    @Column(name = "completion_date")
    private LocalDate completionDate = LocalDate.now();
    
    public BookCompletionRecord(Book book, Member member) {
        this.book = book;
        this.member = member;
        this.id = new BookCompletionId(book.getId(), member.getId());
    }
    
    @Embeddable
    @Data
    @NoArgsConstructor
    public static class BookCompletionId implements Serializable {
        @Column(name = "book_id")
        private Long bookId;
        
        @Column(name = "member_id")
        private Long memberId;
        
        public BookCompletionId(Long bookId, Long memberId) {
            this.bookId = bookId;
            this.memberId = memberId;
        }
    }
}