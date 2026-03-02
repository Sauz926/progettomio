package it.unicas.spring.springai.dto;

import java.util.List;

/**
 * Payload di richiesta per la chat contestuale su un assessment.
 * Usato dal controller assessment e consumato da {@code AssessmentChatService}.
 *
 * @param question domanda utente sul risultato assessment
 * @param history cronologia chat (ultimi turni) inviata dal frontend
 */
public record AssessmentChatRequest(
        String question,
        List<ChatTurn> history
) {
}
