package it.unicas.spring.springai.dto;

import java.util.List;

/**
 * Schema logico dell'output explainable richiesto al modello in fase di assessment.
 * Usato come riferimento concettuale nel flusso di parsing di {@code AssessmentService}.
 *
 * @param riepilogoConformita sintesi testuale della conformità
 * @param nonConformitaRilevate bozze finding con chunkIds di supporto
 * @param raccomandazioni bozze raccomandazioni con chunkIds
 * @param livelloRischio livello rischio complessivo
 * @param punteggioConformita punteggio 0..100
 */
public record ExplainableAssessmentLlmResponse(
        String riepilogoConformita,
        List<ExplainableFindingDraft> nonConformitaRilevate,
        List<ExplainableFindingDraft> raccomandazioni,
        String livelloRischio,
        Integer punteggioConformita
) {
}
