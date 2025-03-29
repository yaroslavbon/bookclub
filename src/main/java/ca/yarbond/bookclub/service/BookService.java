package ca.yarbond.bookclub.service;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookStatus;
import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.model.MemberQueueItem;
import ca.yarbond.bookclub.repository.BookRepository;
import ca.yarbond.bookclub.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final FileStorageService fileStorageService;
    private final MemberQueueService memberQueueService;
    private final MemberRepository memberRepository;

    @Autowired
    public BookService(
            BookRepository bookRepository,
            FileStorageService fileStorageService,
            MemberQueueService memberQueueService,
            MemberRepository memberRepository) {
        this.bookRepository = bookRepository;
        this.fileStorageService = fileStorageService;
        this.memberQueueService = memberQueueService;
        this.memberRepository = memberRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> getBooksByStatus(BookStatus status) {
        return bookRepository.findByStatus(status);
    }

    public List<Book> getBooksByOwner(Member owner) {
        return bookRepository.findByOwner(owner);
    }

    public List<Book> getBooksByOwnerId(Long ownerId) {
        return bookRepository.findByOwnerId(ownerId);
    }

    public List<Book> getBooksByOwnerAndStatus(Member owner, BookStatus status) {
        return bookRepository.findByOwnerAndStatus(owner, status);
    }

    public List<Book> getBooksByOwnerIdAndStatus(Long ownerId, BookStatus status) {
        return bookRepository.findByOwnerIdAndStatus(ownerId, status);
    }

    public List<Book> getRecentlyCompletedBooks() {
        return bookRepository.findTop5RecentlyCompletedBooks();
    }

    public List<Book> getAllCompletedBooks() {
        return bookRepository.findByStatus(BookStatus.COMPLETED);
    }

    public List<Book> searchBooks(String searchTerm) {
        return bookRepository.searchBooks(searchTerm);
    }

    public Book getCurrentBook() {
        List<Book> currentBooks = bookRepository.findByStatus(BookStatus.CURRENT);
        return currentBooks.isEmpty() ? null : currentBooks.get(0);
    }

    public Book getNextBook() {
        List<Book> nextBooks = bookRepository.findByStatus(BookStatus.NEXT);
        return nextBooks.isEmpty() ? null : nextBooks.get(0);
    }

    /**
     * Gets the list of wishlist books for the next member in queue
     */
    public List<Book> getNextMemberWishlistBooks() {
        try {
            MemberQueueItem nextMember = memberQueueService.getNextMember();
            return bookRepository.findByOwnerIdAndStatus(nextMember.getMember().getId(), BookStatus.WISHLIST);
        } catch (RuntimeException e) {
            // No next member found, return empty list
            return List.of();
        }
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
    }

    @Transactional
    public Book createBook(Book book) {
        // Ensure the owner exists
        if (book.getOwner() == null || book.getOwner().getId() == null) {
            throw new RuntimeException("Book must have an owner");
        }

        Member owner = memberRepository.findById(book.getOwner().getId())
                .orElseThrow(() -> new RuntimeException("Owner not found with id: " + book.getOwner().getId()));

        book.setOwner(owner);

        // Set default status if not specified
        if (book.getStatus() == null) {
            book.setStatus(BookStatus.WISHLIST);
        }

        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Long id, Book bookDetails) {
        Book book = getBookById(id);

        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setFiction(bookDetails.isFiction());

        // Only update owner if provided and different
        if (bookDetails.getOwner() != null && bookDetails.getOwner().getId() != null &&
                !book.getOwner().getId().equals(bookDetails.getOwner().getId())) {
            Member newOwner = memberRepository.findById(bookDetails.getOwner().getId())
                    .orElseThrow(() -> new RuntimeException("Owner not found with id: " + bookDetails.getOwner().getId()));
            book.setOwner(newOwner);
        }

        // Only update status if provided and different
        if (bookDetails.getStatus() != null && book.getStatus() != bookDetails.getStatus()) {
            book.setStatus(bookDetails.getStatus());
        }

        book.setComments(bookDetails.getComments());

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }

    @Transactional
    public Book addBookFile(Long bookId, MultipartFile file) {
        Book book = getBookById(bookId);

        String filename = fileStorageService.storeBookFile(file);
        String fileFormat = getFileFormat(file.getOriginalFilename());
        book.addFilePath(fileFormat, filename);

        // If it's an EPUB file, try to extract the cover
        if ("epub".equalsIgnoreCase(fileFormat)) {
            String coverPath = fileStorageService.extractCoverFromEpub(file);
            if (coverPath != null) {
                book.setCoverImagePath(coverPath);
            }
        }

        return bookRepository.save(book);
    }

    /**
     * Marks current book as completed and promotes next book
     */
    @Transactional
    public void completeCurrentBook() {
        Book currentBook = getCurrentBook();
        if (currentBook == null) {
            throw new RuntimeException("No book is currently being read");
        }

        // Mark current book as completed
        currentBook.setStatus(BookStatus.COMPLETED);
        currentBook.setCompletionDate(LocalDate.now());
        bookRepository.save(currentBook);

        Member owner = currentBook.getOwner();

        if (owner == null) {
            throw new RuntimeException("Book with no owner found in DB");
        }

        owner.setLastPickDate(LocalDate.now());
        owner.setTotalPicks(owner.getTotalPicks() + 1);
        memberRepository.save(owner);

        if(owner.isActive()){
            memberQueueService.rotateQueue();
        }
    }

    /**
     * Performs an emergency book replacement
     */
    @Transactional
    public void replaceCurrentBook(Long newBookId) {
        Book currentBook = getCurrentBook();
        if (currentBook == null) {
            throw new RuntimeException("No book is currently being read");
        }

        // Move current book back to wishlist
        currentBook.setStatus(BookStatus.WISHLIST);
        bookRepository.save(currentBook);

        // Set new book as current
        Book newBook = getBookById(newBookId);
        newBook.setStatus(BookStatus.CURRENT);
        bookRepository.save(newBook);
    }

    /**
     * Skips the current book and moves to the next member's selection
     */
    @Transactional
    public void skipCurrentBook(Long placeAfterId) {
        Book currentBook = getCurrentBook();
        if (currentBook == null) {
            throw new RuntimeException("No book is currently being read");
        }

        // Skip the current member in queue
        memberQueueService.skipCurrentMember(placeAfterId);

        // Move current book back to wishlist
        currentBook.setStatus(BookStatus.WISHLIST);
        bookRepository.save(currentBook);
    }

    /**
     * Sets a book as the next to read
     */
    @Transactional
    public Book setNextBook(Long bookId) {
        // First, set any next book back to WISHLIST
        List<Book> nextBooks = bookRepository.findByStatus(BookStatus.NEXT);
        for (Book nextBook : nextBooks) {
            nextBook.setStatus(BookStatus.WISHLIST);
            bookRepository.save(nextBook);
        }

        // Then set the selected book to NEXT
        Book newNextBook = getBookById(bookId);
        newNextBook.setStatus(BookStatus.NEXT);
        return bookRepository.save(newNextBook);
    }

    private String getFileFormat(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * Promotes the next book to current if current is empty
     *
     * @return true if promotion occurred, false otherwise
     */
    @Transactional
    public boolean promoteNextBookIfNeeded() {
        Book currentBook = getCurrentBook();
        Book nextBook = getNextBook();

        if (currentBook == null && nextBook != null) {
            // Change status and save
            nextBook.setStatus(BookStatus.CURRENT);
            bookRepository.save(nextBook);

            if(nextBook.getOwner() != null && nextBook.getOwner().isActive()){
                memberQueueService.rotateToMember(nextBook.getOwner());
            }

            return true;
        }

        return false;
    }

    @Transactional
    public Pair<MemberQueueItem, List<Book>> getNextMemberWithWishlistBooks(Book currentBook) {
        boolean skipCurrentMember = currentBook != null && currentBook.getOwner() != null && currentBook.getOwner().isActive();

        List<MemberQueueItem> queue = memberQueueService.getQueue();

        Iterator<MemberQueueItem> iterator = queue.iterator();

        if (skipCurrentMember && iterator.hasNext()) iterator.next();

        while(iterator.hasNext()){
            MemberQueueItem memberQueueItem = iterator.next();
            List<Book> memberWishlistBooks = getBooksByOwnerAndStatus(memberQueueItem.getMember(), BookStatus.WISHLIST);
            if (!memberWishlistBooks.isEmpty()){
                return Pair.of(memberQueueItem, memberWishlistBooks);
            }
        }

        return null;
    }

    /**
     * Skips the current book from an inactive member
     * Moves the book back to wishlist without queue manipulation
     */
    @Transactional
    public void skipInactiveCurrentBook() {
        Book currentBook = getCurrentBook();
        if (currentBook == null) {
            throw new RuntimeException("No book is currently being read");
        }

        if (currentBook.getOwner() == null) {
            throw new RuntimeException("Book with no owner found in DB");
        }

        if (currentBook.getOwner().isActive()) {
            throw new RuntimeException("Book owner is active, use regular skip functionality");
        }

        // Move current book back to wishlist without queue manipulation
        currentBook.setStatus(BookStatus.WISHLIST);
        bookRepository.save(currentBook);
    }
}