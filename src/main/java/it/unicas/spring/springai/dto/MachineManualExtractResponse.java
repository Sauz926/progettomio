package it.unicas.spring.springai.dto;

/**
 * Dati macchina estratti dal manuale PDF.
 * Prodotto da {@code MachineManualExtractionService} e restituito dal controller macchinari.
 *
 * @param nome nome macchina
 * @param produttore produttore identificato
 * @param modello modello identificato
 * @param numeroSerie seriale identificato
 * @param annoProduzione anno produzione estratto
 * @param categoria categoria normalizzata
 * @param descrizione descrizione sintetica macchina
 * @param specificheTecniche specifiche tecniche principali
 */
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
