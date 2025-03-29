package ca.yarbond.bookclub.controller;

import ca.yarbond.bookclub.model.MemberQueueItem;
import ca.yarbond.bookclub.service.MemberQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/queue")
public class QueueController {

    private final MemberQueueService memberQueueService;

    @Autowired
    public QueueController(MemberQueueService memberQueueService) {
        this.memberQueueService = memberQueueService;
    }

    @GetMapping
    public String viewQueue(Model model) {
        List<MemberQueueItem> queueItems = memberQueueService.getQueue();
        model.addAttribute("queueItems", queueItems);

        // Add current and next members for highlighting
        try {
            MemberQueueItem currentMember = memberQueueService.getCurrentMember();
            MemberQueueItem nextMember = memberQueueService.getNextMember();
            model.addAttribute("currentMember", currentMember);
            model.addAttribute("nextMember", nextMember);
        } catch (RuntimeException e) {
            // Queue might be empty
        }
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
}