package ca.yarbond.bookclub.controller;

import ca.yarbond.bookclub.model.Book;
import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.model.MemberQueueItem;
import ca.yarbond.bookclub.service.BookService;
import ca.yarbond.bookclub.service.MemberQueueService;
import ca.yarbond.bookclub.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/queue")
public class QueueController {

    private final MemberQueueService memberQueueService;
    private final MemberService memberService;
    private final BookService bookService;

    @Autowired
    public QueueController(MemberQueueService memberQueueService, MemberService memberService, BookService bookService) {
        this.memberQueueService = memberQueueService;
        this.memberService = memberService;
        this.bookService = bookService;
    }

    @GetMapping
    public String viewQueue(Model model) {
        // Get queue items for the queue order table
        List<MemberQueueItem> queueItems = memberQueueService.getQueue();
        model.addAttribute("queueItems", queueItems);

        // Add current and next members for highlighting
        int queueSize = queueItems.size();
        if (queueSize > 0) {
            model.addAttribute("currentMember", queueItems.get(0));
            if (queueSize > 1) {
                model.addAttribute("nextMember", queueItems.get(1));
            }
        }

        // Get all members for the member management table
        List<Member> allMembers = memberService.getAllMembers();
        model.addAttribute("allMembers", allMembers);

        List<Book> allBooks = bookService.getAllBooks();

        // Initialize count maps
        Map<Long, Integer> wishlistCounts = new HashMap<>();
        Map<Long, Integer> bookCounts = new HashMap<>();

        // Process all books once to update both counts
        for (Book book : allBooks) {
            if (book.getOwner() != null) {
                Long ownerId = book.getOwner().getId();

                // Update total book count
                bookCounts.put(ownerId, bookCounts.getOrDefault(ownerId, 0) + 1);

                // Update wishlist count if applicable
                if (book.getStatus() == ca.yarbond.bookclub.model.BookStatus.WISHLIST) {
                    wishlistCounts.put(ownerId, wishlistCounts.getOrDefault(ownerId, 0) + 1);
                }
            }
        }

        model.addAttribute("wishlistCounts", wishlistCounts);
        model.addAttribute("bookCounts", bookCounts);

        model.addAttribute("activeTab", "queue");
        return "queue/view";
    }

    @PostMapping("/rotate")
    public String rotateQueue(RedirectAttributes redirectAttributes) {
        try {
            memberQueueService.rotateQueue();
            redirectAttributes.addFlashAttribute("successMessage", "Queue rotated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/queue";
    }

    @PostMapping("/member/{id}/move-up")
    public String moveMemberUp(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            MemberQueueItem queueItem = memberQueueService.getQueueItemByMemberId(id);
            if (queueItem.getPosition() > 0) {
                memberQueueService.moveMemberToPosition(id, queueItem.getPosition() - 1);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Member '" + queueItem.getMember().getName() + "' moved up");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Member '" + queueItem.getMember().getName() + "' is already at the top");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/queue";
    }

    @PostMapping("/member/{id}/move-down")
    public String moveMemberDown(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            MemberQueueItem queueItem = memberQueueService.getQueueItemByMemberId(id);
            List<MemberQueueItem> allItems = memberQueueService.getQueue();
            if (queueItem.getPosition() < allItems.size() - 1) {
                memberQueueService.moveMemberToPosition(id, queueItem.getPosition() + 1);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Member '" + queueItem.getMember().getName() + "' moved down");
            } else {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Member '" + queueItem.getMember().getName() + "' is already at the bottom");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/queue";
    }

    // New Member Management Methods

    @PostMapping("/members")
    public String createMember(@ModelAttribute Member member,
                               RedirectAttributes redirectAttributes) {
        try {
            // Generate a random password
            String generatedPassword = memberService.generateRandomPassword();
            Member createdMember = memberService.createMember(member, generatedPassword);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + createdMember.getName() + "' created and added to queue");
            redirectAttributes.addFlashAttribute("generatedPassword", generatedPassword);
            redirectAttributes.addFlashAttribute("newMemberId", createdMember.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/queue";
    }

    @PostMapping("/members/{id}")
    public String updateMemberName(@PathVariable Long id, @ModelAttribute Member member,
                                   RedirectAttributes redirectAttributes) {
        try {
            Member updatedMember = memberService.updateMemberName(id, member.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + updatedMember.getName() + "' updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/queue";
    }

    @PostMapping("/members/{id}/toggle-status")
    public String toggleMemberStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Member updatedMember = memberService.toggleStatus(id);

            // Manage queue membership based on active status
            if (updatedMember.isActive()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Member '" + updatedMember.getName() + "' activated and added to queue");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Member '" + updatedMember.getName() + "' deactivated and removed from queue");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/queue";
    }

    @PostMapping("/members/{id}/delete")
    public String deleteMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Check if member has books
            int bookCount = bookService.getBooksByOwnerId(id).size();
            if (bookCount > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Cannot delete member: Member has " + bookCount + " books associated with them");
                return "redirect:/queue";
            }

            String deletedMemberName = memberService.deleteMember(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + deletedMemberName + "' deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/queue";
    }
    
    @PostMapping("/members/{id}/reset-password")
    public String resetMemberPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Member member = memberService.getMemberById(id);
            String newPassword = memberService.resetPassword(id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Password for member '" + member.getName() + "' has been reset");
            redirectAttributes.addFlashAttribute("resetPasswordFor", member.getId());
            redirectAttributes.addFlashAttribute("resetPassword", newPassword);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/queue";
    }

}