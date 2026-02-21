package it.unicas.spring.springai.dto;

import java.util.List;

public record ExplainableAssessmentLlmResponse(
        String riepilogoConformita,
        List<ExplainableFindingDraft> nonConformitaRilevate,
        List<ExplainableFindingDraft> raccomandazioni,
        String livelloRischio,
        Integer punteggioConformita
) {
}

