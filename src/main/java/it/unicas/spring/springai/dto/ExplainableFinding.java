package it.unicas.spring.springai.dto;

import java.util.List;

/**
 * Finding explainable finale con fonti risolte.
 * Prodotto da {@code AssessmentService} e riusato da {@code AssessmentChatService}.
 *
 * @param text descrizione finding/non conformità/raccomandazione
 * @param sources fonti che supportano il finding
 */
public record ExplainableFinding(
        String text,
        List<ExplainableSource> sources
) {
}
