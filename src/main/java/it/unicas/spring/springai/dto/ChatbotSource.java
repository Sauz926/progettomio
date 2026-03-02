package it.unicas.spring.springai.dto;

/**
 * Fonte restituita dal chatbot generale per spiegare da dove arriva la risposta.
 * Costruita da {@code ChatbotService} e serializzata da {@code ChatbotController}.
 *
 * @param reference riferimento sintetico (file/pagina)
 * @param fileName nome documento sorgente
 * @param page pagina sorgente, se disponibile
 * @param chunk estratto del chunk usato
 * @param confidence confidenza retrieval associata al chunk
 */
public record ChatbotSource(
        String reference,
        String fileName,
        Integer page,
        String chunk,
        Double confidence
) {
}
