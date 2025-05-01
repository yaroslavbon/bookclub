package ca.yarbond.bookclub.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to initialize the admin mode session variable for all requests
 */
public class AdminModeInterceptor implements HandlerInterceptor {

    private static final String ADMIN_MODE_SESSION_KEY = "adminModeEnabled";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession();
        
        // Initialize adminModeEnabled to false if not present
        if (session.getAttribute(ADMIN_MODE_SESSION_KEY) == null) {
            session.setAttribute(ADMIN_MODE_SESSION_KEY, false);
        }
        
        return true; // Continue with the request
    }
}