package it.unicas.spring.springai.dto;

/**
 * Risposta REST per la chat contestuale assessment.
 * Creata dal controller assessment dopo la chiamata a {@code AssessmentChatService}.
 *
 * @param answer risposta testuale contestualizzata
 */
public record AssessmentChatResponse(
        String answer
) {
}
