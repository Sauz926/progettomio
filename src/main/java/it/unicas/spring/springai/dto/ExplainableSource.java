package it.unicas.spring.springai.dto;

/**
 * Fonte normativa associata a un finding explainable.
 * Prodotto da {@code AssessmentService} e letto da {@code AssessmentChatService}.
 *
 * @param reference riferimento umano (documento/articolo/pagina)
 * @param fileName nome del documento sorgente
 * @param page pagina del documento, se disponibile
 * @param chunk estratto testuale del chunk a supporto
 * @param confidence confidenza retrieval stimata
 */
public record ExplainableSource(
        String reference,
        String fileName,
        Integer page,
        String chunk,
        Double confidence
) {
}
