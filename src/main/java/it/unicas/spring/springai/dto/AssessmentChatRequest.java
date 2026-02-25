package it.unicas.spring.springai.dto;

import java.util.List;

public record AssessmentChatRequest(
        String question,
        List<ChatTurn> history
) {
}

