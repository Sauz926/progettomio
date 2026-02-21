package it.unicas.spring.springai.dto;

import java.util.List;

public record ExplainableFindingDraft(
        String text,
        List<Integer> chunkIds
) {
}

