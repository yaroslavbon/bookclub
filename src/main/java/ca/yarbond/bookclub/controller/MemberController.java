package ca.yarbond.bookclub.controller;

import ca.yarbond.bookclub.model.Member;
import ca.yarbond.bookclub.service.MemberService;
import ca.yarbond.bookclub.service.MemberQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberQueueService memberQueueService;

    @Autowired
    public MemberController(MemberService memberService, MemberQueueService memberQueueService) {
        this.memberService = memberService;
        this.memberQueueService = memberQueueService;
    }

    @GetMapping
    public String getAllMembers(Model model) {
        model.addAttribute("members", memberService.getAllMembers());
        model.addAttribute("newMember", new Member());
        model.addAttribute("activeTab", "members");
        return "members/list";
    }

    @GetMapping("/{id}")
    public String getMemberById(@PathVariable Long id, Model model) {
        model.addAttribute("member", memberService.getMemberById(id));
        model.addAttribute("activeTab", "members");
        return "members/detail";
    }

    @PostMapping
    public String createMember(@ModelAttribute Member member,
                               @RequestParam(required = false) boolean addToQueue,
                               RedirectAttributes redirectAttributes) {
        try {
            Member createdMember = memberService.createMember(member);

            // Add to queue if requested
            if (addToQueue) {
                memberQueueService.addMemberToQueue(createdMember.getId());
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + createdMember.getName() + "' created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}")
    public String updateMember(@PathVariable Long id, @ModelAttribute Member member,
                               RedirectAttributes redirectAttributes) {
        try {
            Member updatedMember = memberService.updateMember(id, member);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + updatedMember.getName() + "' updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}/delete")
    public String deleteMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Member member = memberService.getMemberById(id);
            String memberName = member.getName();
            memberService.deleteMember(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + memberName + "' deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}/add-to-queue")
    public String addMemberToQueue(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Member member = memberService.getMemberById(id);
            memberQueueService.addMemberToQueue(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + member.getName() + "' added to queue successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}/remove-from-queue")
    public String removeMemberFromQueue(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Member member = memberService.getMemberById(id);
            memberQueueService.removeMemberFromQueue(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + member.getName() + "' removed from queue successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/members";
    }
}