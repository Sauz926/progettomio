package it.unicas.spring.springai.dto;

import java.util.List;

public record ChatbotChatResponse(
        String answer,
        List<ChatbotSource> sources
) {
}

