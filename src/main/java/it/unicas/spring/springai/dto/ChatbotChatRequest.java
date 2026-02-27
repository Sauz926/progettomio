package it.unicas.spring.springai.dto;

import java.util.List;

public record ChatbotChatRequest(
        String question,
        List<ChatTurn> history
) {
}

