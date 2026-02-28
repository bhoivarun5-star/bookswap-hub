package com.example.demo.config;

import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class NotificationInterceptor implements HandlerInterceptor {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationInterceptor(NotificationService notificationService,
                                   UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler,
                           @Nullable ModelAndView modelAndView) {
        if (modelAndView == null) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            userRepository.findByUsername(auth.getName()).ifPresent(user ->
                    modelAndView.addObject("unreadNotificationCount",
                            notificationService.getUnreadCount(user)));
        }
    }
}
