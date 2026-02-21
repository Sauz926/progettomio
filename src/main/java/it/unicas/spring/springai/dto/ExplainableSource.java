package it.unicas.spring.springai.dto;

public record ExplainableSource(
        String reference,
        String fileName,
        Integer page,
        String chunk,
        Double confidence
) {
}

