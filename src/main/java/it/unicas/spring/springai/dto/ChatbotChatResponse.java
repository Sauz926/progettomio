package it.unicas.spring.springai.dto;

import java.util.List;

/**
 * Risposta REST della chat RAG generica.
 * Costruita da {@code ChatbotService} e restituita da {@code ChatbotController}.
 *
 * @param answer testo della risposta assistente
 * @param sources fonti/chunk usati per la risposta
 */
public record ChatbotChatResponse(
        String answer,
        List<ChatbotSource> sources
) {
}
