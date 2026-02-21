package it.unicas.spring.springai.dto;

import java.util.List;

public record ExplainableFinding(
        String text,
        List<ExplainableSource> sources
) {
}

