package it.unicas.spring.springai.dto;

public record ChatbotSource(
        String reference,
        String fileName,
        Integer page,
        String chunk,
        Double confidence
) {
}

