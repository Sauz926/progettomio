package it.unicas.spring.springai.dto;

public record MachineManualExtractResponse(
        String nome,
        String produttore,
        String modello,
        String numeroSerie,
        Integer annoProduzione,
        String categoria,
        String descrizione,
        String specificheTecniche
) {
}

