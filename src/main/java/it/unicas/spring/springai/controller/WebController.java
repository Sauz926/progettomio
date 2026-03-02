package it.unicas.spring.springai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    /**
     * Reindirizza la root applicativa verso la pagina principale.
     * Chiamata da Spring MVC quando il browser richiede {@code GET /}; usata come entry-point UI.
     *
     * @return redirect HTTP verso {@code /index.html}
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }

    /**
     * Restituisce la view di login.
     * Chiamata da Spring MVC tramite {@code GET /login}; utilizzata dal flusso di autenticazione Spring Security.
     *
     * @return nome template della pagina di login
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
