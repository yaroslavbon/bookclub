package ca.yarbond.bookclub.controller;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.BookStatus;
import ca.yarbond.bookclub.model.MemberQueueItem;
import ca.yarbond.bookclub.service.BookService;
import ca.yarbond.bookclub.service.MemberQueueService;
import ca.yarbond.bookclub.service.MemberService;
import ca.yarbond.bookclub.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MainController {

    private final BookService bookService;
    private final RatingService ratingService;
    private final MemberQueueService memberQueueService;
    private final MemberService memberService;

    @Autowired
    public MainController(BookService bookService, RatingService ratingService,
                          MemberQueueService memberQueueService, MemberService memberService) {
        this.bookService = bookService;
        this.ratingService = ratingService;
        this.memberQueueService = memberQueueService;
        this.memberService = memberService;
    }

    @GetMapping("/")
    public String index(Model model) {
        // Auto-promote next book if needed
        boolean promoted = bookService.promoteNextBookIfNeeded();

        // Get current and next books (after possible promotion)
        Book currentBook = bookService.getCurrentBook();
        Book nextBook = bookService.getNextBook();
        if (promoted && currentBook != null) {
            model.addAttribute("successMessage",
                    "\"" + currentBook.getTitle() + "\" has been automatically promoted to current reading.");
        }
        model.addAttribute("currentBook", currentBook);
        model.addAttribute("nextBook", nextBook);

        // Get current member's other wishlist books (for replace functionality)
        if (currentBook != null && currentBook.getOwner() != null) {
            List<Book> currentMemberWishlistBooks = bookService.getBooksByOwnerIdAndStatus(
                    currentBook.getOwner().getId(), BookStatus.WISHLIST);
            model.addAttribute("currentMemberWishlistBooks", currentMemberWishlistBooks);
        }

        // Get next members books for selection
        Pair<MemberQueueItem, List<Book>> pair = bookService.getNextMemberWithWishlistBooks(currentBook == null);

        if(pair != null) {
            model.addAttribute("nextMember", pair.getFirst());
            model.addAttribute("nextMemberBooks", pair.getSecond());
        }

        // Get recently completed books
        List<Book> recentlyCompletedBooks = bookService.getRecentlyCompletedBooks();
        model.addAttribute("recentlyCompletedBooks", recentlyCompletedBooks);

        // Get average ratings for completed books
        if (!recentlyCompletedBooks.isEmpty()) {
            List<Long> bookIds = recentlyCompletedBooks.stream()
                    .map(Book::getId)
                    .collect(Collectors.toList());

            Map<Long, Map<String, Double>> bookRatings = ratingService.getAverageRatingsForBooks(bookIds);
            model.addAttribute("bookRatings", bookRatings);
        }

        // Add queue members
        List<MemberQueueItem> queueMembers = memberQueueService.getQueue();
        model.addAttribute("queueMembers", queueMembers);
        model.addAttribute("activeTab", "home");
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // Handle book completion via form POST
    @PostMapping("/books/complete-current")
    public String completeCurrentBook() {
        bookService.completeCurrentBook();
        return "redirect:/";
    }

    // Handle emergency replacements via form POST
    @PostMapping("/books/replace-current")
    public String replaceCurrentBook(@RequestParam("newBookId") Long newBookId) {
        bookService.replaceCurrentBook(newBookId);
        return "redirect:/";
    }

    // Handle book skipping via form POST
    @PostMapping("/books/skip-current")
    public String skipCurrentBook(@RequestParam("placeAfterId") Long placeAfterId) {
        bookService.skipCurrentBook(placeAfterId);
        return "redirect:/";
    }
}