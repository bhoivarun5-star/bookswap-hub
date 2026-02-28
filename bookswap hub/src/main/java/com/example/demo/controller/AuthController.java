package com.example.demo.controller;

import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService,
            @Lazy AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    // ─── Admin Login (hidden) ─────────────────────────────────────────────────

    @GetMapping("/admin-login")
    public String adminLoginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        return "admin/login";
    }

    @PostMapping("/admin-login")
    public String adminLoginSubmit(@RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            RedirectAttributes ra) {
        try {
            // Authenticate credentials
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            // Must be ROLE_ADMIN
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return "redirect:/admin-login?error=true";
            }

            // Store authentication in security context + session
            SecurityContext sc = SecurityContextHolder.createEmptyContext();
            sc.setAuthentication(auth);
            SecurityContextHolder.setContext(sc);

            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

            return "redirect:/admin/panel";

        } catch (AuthenticationException ex) {
            return "redirect:/admin-login?error=true";
        }
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been successfully logged out.");
        }
        return "login";
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/register";
        }
        if (username.trim().length() < 3) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username must be at least 3 characters.");
            return "redirect:/register";
        }
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters.");
            return "redirect:/register";
        }
        try {
            userService.registerUser(username.trim(), email.trim(), password);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account created successfully! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/register";
        }
    }

    // ─── Root redirect ────────────────────────────────────────────────────────

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }
}
