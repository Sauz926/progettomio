package it.unicas.spring.springai.dto;

import java.util.List;

/**
 * Bozza finding restituita dal modello prima della risoluzione completa delle fonti.
 * Usata da {@code AssessmentService} durante il parsing explainable.
 *
 * @param text testo del finding
 * @param chunkIds id chunk citati dal modello
 */
public record ExplainableFindingDraft(
        String text,
        List<Integer> chunkIds
) {
}
