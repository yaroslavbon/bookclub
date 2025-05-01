package ca.yarbond.bookclub.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Collection;

@Controller
@RequestMapping("/admin-mode")
public class AdminToggleController {

    private static final String ADMIN_MODE_SESSION_KEY = "adminModeEnabled";

    @PostMapping("/toggle")
    public String toggleAdminMode(HttpSession session, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        
        // Check if user has ADMIN role
        boolean isAdmin = authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                    "You don't have permission to toggle admin mode");
            return "redirect:/";
        }
        
        // Toggle admin mode in session
        Boolean currentMode = (Boolean) session.getAttribute(ADMIN_MODE_SESSION_KEY);
        boolean newMode = currentMode == null || !currentMode;
        session.setAttribute(ADMIN_MODE_SESSION_KEY, newMode);
        
        if (newMode) {
            redirectAttributes.addFlashAttribute("successMessage", "Admin mode enabled");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Admin mode disabled");
        }
        
        return "redirect:/";
    }
}