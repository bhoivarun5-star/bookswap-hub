package com.example.demo.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("statusText",
                statusCode == 404 ? "Page Not Found" : statusCode == 403 ? "Access Denied" : "Something Went Wrong");
        model.addAttribute("message",
                statusCode == 404 ? "The page you're looking for doesn't exist."
                        : statusCode == 403 ? "You don't have permission to access this page."
                                : "An unexpected error occurred. Please try again.");

        return "error";
    }
}
