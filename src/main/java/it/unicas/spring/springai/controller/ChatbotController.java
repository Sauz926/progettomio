package it.unicas.spring.springai.controller;

import it.unicas.spring.springai.dto.ChatbotChatRequest;
import it.unicas.spring.springai.dto.ChatbotChatResponse;
import it.unicas.spring.springai.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Espone il prompt di sistema di default usato dal chatbot.
     * Chiamata da Spring MVC tramite {@code GET /api/chatbot/system-prompt}; usa
     * {@link ChatbotService#defaultSystemPrompt()}.
     *
     * @return prompt di sistema corrente
     */
    @GetMapping("/system-prompt")
    public ResponseEntity<?> systemPrompt() {
        return ResponseEntity.ok(Map.of("systemPrompt", chatbotService.defaultSystemPrompt()));
    }

    /**
     * Gestisce una richiesta di chat RAG e restituisce risposta con fonti.
     * Chiamata da Spring MVC tramite {@code POST /api/chatbot/chat}; delega a
     * {@link ChatbotService#chat(ChatbotChatRequest)}.
     *
     * @param request domanda utente, storia conversazione e prompt opzionale
     * @return risposta chatbot o errore di validazione/esecuzione
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatbotChatRequest request) {
        try {
            ChatbotChatResponse response = chatbotService.chat(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Errore durante la chat";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Errore durante la chat: " + e.getMessage()));
        }
    }
}
