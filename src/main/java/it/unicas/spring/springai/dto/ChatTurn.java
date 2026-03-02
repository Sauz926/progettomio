package it.unicas.spring.springai.dto;

/**
 * Singolo turno di conversazione (utente o assistente).
 * Usato nei payload chat di {@code ChatbotService} e {@code AssessmentChatService}.
 *
 * @param role ruolo mittente (user/assistant)
 * @param content contenuto del messaggio
 */
public record ChatTurn(
        String role,
        String content
) {
}
