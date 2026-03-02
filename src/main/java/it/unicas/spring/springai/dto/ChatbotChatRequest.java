package it.unicas.spring.springai.dto;

import java.util.List;

/**
 * Payload di richiesta per la chat RAG generale.
 * Ricevuto dal controller chatbot e consumato da {@code ChatbotService}.
 *
 * @param question domanda principale utente
 * @param history ultimi turni conversazione
 * @param systemPrompt istruzioni custom opzionali per il modello
 */
public record ChatbotChatRequest(
        String question,
        List<ChatTurn> history,
        String systemPrompt
) {
}
