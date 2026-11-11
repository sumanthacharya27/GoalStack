package com.goalstack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * When the React build is packaged inside the jar, a browser refresh on /goals/5 must still load the app.
 * These page URLs belong to React Router, so the server answers them with index.html.
 */
@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/register", "/goals", "/goals/**", "/subgoals/**"})
    public String forwardToReact() {
        return "forward:/index.html";
    }
}
