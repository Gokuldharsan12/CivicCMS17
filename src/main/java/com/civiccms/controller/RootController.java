package com.civiccms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles the root URL "/" and redirects to login.html.
 * The index.html auth guard then redirects to login if not authenticated.
 */
@Controller
public class RootController {

    /**
     * Visiting http://localhost:PORT/ → redirect to /login.html
     * After login, the user lands on /index.html (home).
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/login.html";
    }
}
