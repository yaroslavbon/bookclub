package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookCompletionRecord;
import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.repository.BookCompletionRepository;
import ca.yarbond.bookclub.repository.BookRepository;
import ca.yarbond.bookclub.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookCompletionService {

    private final BookCompletionRepository bookCompletionRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final BookService bookService;
    private final MemberService memberService;

    @Autowired
    public BookCompletionService(
            BookCompletionRepository bookCompletionRepository,
            BookRepository bookRepository,
            MemberRepository memberRepository,
            BookService bookService,
            MemberService memberService) {
        this.bookCompletionRepository = bookCompletionRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.bookService = bookService;
        this.memberService = memberService;
    }

    @Transactional
    public BookCompletionRecord markBookAsRead(Long bookId, Long memberId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));
        
        // Check if already marked as read
        if (bookCompletionRepository.existsByBookIdAndMemberId(bookId, memberId)) {
            throw new RuntimeException("Book already marked as read by this member");
        }
        
        BookCompletionRecord record = new BookCompletionRecord(book, member);
        record.setCompletionDate(LocalDate.now());
        
        return bookCompletionRepository.save(record);
    }
    
    @Transactional(readOnly = true)
    public int getReadCountForBook(Long bookId) {
        return bookCompletionRepository.countCompletionsForBook(bookId);
    }
    
    @Transactional(readOnly = true)
    public List<BookCompletionRecord> getReadRecordsForBook(Long bookId) {
        return bookCompletionRepository.findByBookId(bookId);
    }
    
    /**
     * Gets the list of members who have read a book
     */
    @Transactional(readOnly = true)
    public List<Member> getMembersWhoReadBook(Long bookId) {
        List<BookCompletionRecord> records = bookCompletionRepository.findByBookId(bookId);
        return records.stream()
                .map(BookCompletionRecord::getMember)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public boolean hasReadBook(Long bookId, Long memberId) {
        return bookCompletionRepository.existsByBookIdAndMemberId(bookId, memberId);
    }
    
    /**
     * Removes a book completion record for a member
     */
    @Transactional
    public void removeBookCompletionRecord(Long bookId, Long memberId) {
        BookCompletionRecord.BookCompletionId id = new BookCompletionRecord.BookCompletionId(bookId, memberId);
        bookCompletionRepository.deleteById(id);
    }
    
    /**
     * Auto-mark a book as read for all members when it's completed
     * (Used for existing completed books in the database)
     */
    @Transactional
    public void markCompletedBookAsReadByAllMembers(Long bookId) {
        List<Member> activeMembers = memberService.getActiveMembers();
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));
        
        if (book.getStatus() != ca.yarbond.bookclub.model.BookStatus.COMPLETED) {
            return;
        }
        
        for (Member member : activeMembers) {
            if (!bookCompletionRepository.existsByBookIdAndMemberId(bookId, member.getId())) {
                BookCompletionRecord record = new BookCompletionRecord(book, member);
                record.setCompletionDate(book.getCompletionDate() != null ? 
                                         book.getCompletionDate() : LocalDate.now());
                bookCompletionRepository.save(record);
            }
        }
    }
    
    /**
     * Calculate the minimum number of readers required to mark a book as completed.
     * Logic: All but one member (total active members - 1), with a minimum of 1.
     */
    @Transactional(readOnly = true)
    public int getRequiredReadersCount() {
        int activeMembers = memberService.getActiveMembers().size();
        return Math.max(1, activeMembers - 1); // All but one member, minimum 1
    }
    
    /**
     * Check if a book can be marked as completed based on the number of readers.
     * Returns true if enough members have read the book.
     */
    @Transactional(readOnly = true)
    public boolean isBookCompletable(Long bookId) {
        int readCount = getReadCountForBook(bookId);
        int requiredReaders = getRequiredReadersCount();
        return readCount >= requiredReaders;
    }
}