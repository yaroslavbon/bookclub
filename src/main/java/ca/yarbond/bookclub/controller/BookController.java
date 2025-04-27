package ca.yarbond.bookclub.controller;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookStatus;
import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.model.Rating;
import ca.yarbond.bookclub.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ca.yarbond.bookclub.service.BookSearchService.DEFAULT_PAGE_SIZE;

@Controller
@RequestMapping("/books")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;
    private final MemberService memberService;
    private final RatingService ratingService;
    private final FileStorageService fileStorageService;
    private final BookSearchService bookSearchService;

    @Autowired
    public BookController(BookService bookService, MemberService memberService, 
                         RatingService ratingService, FileStorageService fileStorageService,
                         BookSearchService bookSearchService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.ratingService = ratingService;
        this.fileStorageService = fileStorageService;
        this.bookSearchService = bookSearchService;
    }

    @GetMapping
    public String getAllBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        
        // Use the search service to get books with applied filters
        Page<Book> bookPage = bookSearchService.searchBooks(search, status, ownerId, page);
        
        // Add the book list and pagination data to the model
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalItems", bookPage.getTotalElements());

        // Add pagination info
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", DEFAULT_PAGE_SIZE);
        
        // Include current filter parameters for pagination links
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("ownerId", ownerId);
        
        // Other attributes
        model.addAttribute("newBook", new Book());
        model.addAttribute("members", memberService.getAllMembers());
        model.addAttribute("statuses", BookStatus.values());
        model.addAttribute("activeTab", "books");

        return "books/list";
    }

    @GetMapping("/{id}")
    public String getBookById(@PathVariable Long id, Model model) {
        Book book = bookService.getBookById(id);
        List<Rating> ratings = ratingService.getRatingsByBookId(id);
        Map<String, Double> averageRatings = ratingService.getAverageRatings(id);

        model.addAttribute("book", book);
        model.addAttribute("ratings", ratings);
        model.addAttribute("averageRatings", averageRatings);
        model.addAttribute("members", memberService.getAllMembers());
        model.addAttribute("activeTab", "books");

        return "books/detail";
    }

    /**
     * Show the Add Book form
     */
    @GetMapping("/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("members", memberService.getAllMembers());
        return "books/add";
    }

    /**
     * Show the Edit Book form
     */
    @GetMapping("/{id}/edit")
    public String showEditBookForm(@PathVariable Long id, Model model) {
        Book book = bookService.getBookById(id);
        model.addAttribute("book", book);
        model.addAttribute("members", memberService.getAllMembers());
        return "books/edit";
    }

    @PostMapping
    public String createBook(@ModelAttribute Book book, @RequestParam(required = false) String coverUrl, RedirectAttributes redirectAttributes) {
        try {
            // Get the actual Member object from the database
            if (book.getOwner() != null && book.getOwner().getId() != null) {
                Member owner = memberService.getMemberById(book.getOwner().getId());
                book.setOwner(owner);
            }

            // Set default status if not provided
            if (book.getStatus() == null) {
                book.setStatus(BookStatus.WISHLIST);
            }

            // Make sure pageCount is handled properly (can be null)
            if (book.getPageCount() != null && book.getPageCount() <= 0) {
                book.setPageCount(null);
            }

            Book createdBook = bookService.createBook(book);

            if (coverUrl != null && !coverUrl.isEmpty()) {
                try {
                    fileStorageService.downloadRemoteCoverImage(createdBook, coverUrl);
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Book '" + createdBook.getTitle() + "' created successfully with cover image");
                } catch (Exception e) {
                    // Just log the error but continue - the book was created successfully
                    logger.error("Failed to download cover image: {}", e.getMessage());
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Book '" + createdBook.getTitle() + "' created successfully, but cover image could not be downloaded");
                }
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Book '" + createdBook.getTitle() + "' created successfully");
            }
            return "redirect:/books/" + createdBook.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/books/add";
        }
    }

    @PostMapping("/{id}")
    public String updateBook(@PathVariable Long id, @ModelAttribute Book book,
                             @RequestParam(required = false) String coverUrl,
                             RedirectAttributes redirectAttributes) {
        try {
            // Get the actual Member object from the database if provided
            if (book.getOwner() != null && book.getOwner().getId() != null) {
                Member owner = memberService.getMemberById(book.getOwner().getId());
                book.setOwner(owner);
            }

            // Make sure pageCount is handled properly (can be null)
            if (book.getPageCount() != null && book.getPageCount() <= 0) {
                book.setPageCount(null);
            }

            Book updatedBook = bookService.updateBook(id, book);

            // Handle remote cover image if provided
            if (coverUrl != null && !coverUrl.isEmpty()) {
                try {
                    fileStorageService.downloadRemoteCoverImage(updatedBook, coverUrl);
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Book '" + updatedBook.getTitle() + "' updated successfully with new cover image");
                } catch (Exception e) {
                    // Just log the error but continue - the book was updated successfully
                    logger.error("Failed to download cover image: {}", e.getMessage());
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Book '" + updatedBook.getTitle() + "' updated successfully, but cover image could not be downloaded");
                }
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Book '" + updatedBook.getTitle() + "' updated successfully");
            }
            return "redirect:/books/" + updatedBook.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/books/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Book book = bookService.getBookById(id);
            String bookTitle = book.getTitle();
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Book '" + bookTitle + "' deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/books";
    }

    @PostMapping("/{id}/upload")
    public String uploadBookFile(@PathVariable Long id,
                                 @RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            bookService.addBookFile(id, file);
            redirectAttributes.addFlashAttribute("successMessage", "File uploaded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload file: " + e.getMessage());
        }
        return "redirect:/books/" + id;
    }

    @PostMapping("/{id}/rate")
    public String rateBook(@PathVariable Long id,
                           @RequestParam Long memberId,
                           @RequestParam Integer readabilityRating,
                           @RequestParam Integer contentRating,
                           @RequestParam(required = false) String comments,
                           RedirectAttributes redirectAttributes) {
        try {
            ratingService.createOrUpdateRating(id, memberId, readabilityRating, contentRating, comments);
            redirectAttributes.addFlashAttribute("successMessage", "Rating submitted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to submit rating: " + e.getMessage());
        }
        return "redirect:/books/" + id;
    }

    @PostMapping("/{id}/did-not-read")
    public String markAsDidNotRead(@PathVariable Long id,
                                   @RequestParam Long memberId,
                                   RedirectAttributes redirectAttributes) {
        try {
            ratingService.markAsDidNotRead(id, memberId);
            redirectAttributes.addFlashAttribute("successMessage", "Marked as 'Did Not Read' successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to mark as 'Did Not Read': " + e.getMessage());
        }
        return "redirect:/books/" + id;
    }

    @PostMapping("/{id}/set-next")
    public String setNextBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Book book = bookService.setNextBook(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    book.getTitle() + " has been set as the next book");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/ratings/{id}/delete")
    public String deleteRating(@PathVariable Long id,
                               @RequestParam Long bookId,
                               RedirectAttributes redirectAttributes) {
        try {
            ratingService.deleteRating(id);
            redirectAttributes.addFlashAttribute("successMessage", "Rating deleted successfully");
            return "redirect:/books/" + bookId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/books/" + bookId;
        }
    }

    @GetMapping("/{id}/ratings-fragment")
    public String getRatingsFragment(@PathVariable Long id, Model model) {
        List<Rating> ratings = ratingService.getRatingsByBookId(id);
        List<Member> members = memberService.getAllMembers();

        // Create a map of memberId -> rating for easy lookup in the template
        Map<Long, Rating> memberRatingsMap = new HashMap<>();
        for (Rating rating : ratings) {
            memberRatingsMap.put(rating.getMember().getId(), rating);
        }

        model.addAttribute("bookId", id);
        model.addAttribute("ratings", ratings);
        model.addAttribute("members", members);
        model.addAttribute("memberRatingsMap", memberRatingsMap);

        return "books/ratings-fragment :: bookRatings";
    }
}