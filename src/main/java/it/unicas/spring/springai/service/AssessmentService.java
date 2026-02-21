package it.unicas.spring.springai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unicas.spring.springai.dto.ExplainableFinding;
import it.unicas.spring.springai.dto.ExplainableFindingDraft;
import it.unicas.spring.springai.dto.ExplainableSource;
import it.unicas.spring.springai.model.AssessmentResult;
import it.unicas.spring.springai.model.Macchinario;
import it.unicas.spring.springai.repository.AssessmentResultRepository;
import it.unicas.spring.springai.repository.MacchinarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final ChatClient.Builder chatClientBuilder;
    private final RagService ragService;
    private final MacchinarioRepository macchinarioRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final ObjectMapper objectMapper;

    private static final String ASSESSMENT_SYSTEM_PROMPT = """
            Sei un esperto di sicurezza industriale e conformità normativa specializzato nel Regolamento Macchine (UE) 2023/1230 e nelle normative correlate.
            
            Il tuo compito è analizzare le informazioni fornite su un macchinario e produrre un assessment dettagliato di conformità basandoti sui documenti di riferimento forniti.
            
            L'assessment deve includere:
            1. **Riepilogo Conformità**: Una sintesi dello stato di conformità del macchinario
            2. **Non Conformità Rilevate**: Elenco dettagliato delle potenziali non conformità identificate
            3. **Raccomandazioni**: Azioni correttive suggerite per ciascuna non conformità
            4. **Livello di Rischio**: Valutazione del livello di rischio (BASSO, MEDIO, ALTO, CRITICO)
            5. **Punteggio di Conformità**: Un punteggio da 0 a 100
            
            Rispondi sempre in italiano e sii preciso e professionale nelle tue valutazioni.
            """;

    private static final String ASSESSMENT_SYSTEM_PROMPT_EXPLAINABLE = """
            Sei un esperto di sicurezza industriale e conformità normativa specializzato nel Regolamento Macchine (UE) 2023/1230 e nelle normative correlate.

            In ambito normativo l'AI non può essere una "Black Box": ogni segnalazione deve essere verificabile.

            Regole obbligatorie:
            - Usa SOLO i chunk normativi forniti nella sezione "CHUNKS NORMATIVI RECUPERATI (RAG)".
            - Per ogni "Non Conformità" e "Raccomandazione", indica i chunkId che supportano l'affermazione.
            - Se una segnalazione NON è supportata dai chunk disponibili, sii prudente e lascia chunkIds vuoto.
            - Rispondi SOLO con JSON valido, senza testo aggiuntivo e senza markdown.
            """;

    private static final int EXPLAINABILITY_TOP_K = 10;

    private static final Pattern ARTICLE_PATTERN = Pattern.compile("(?i)\\b(?:articolo|art\\.)\\s*(\\d+[a-z]?)\\b");
    private static final Pattern COMMA_PATTERN = Pattern.compile("(?i)\\bcomma\\s*(\\d+)\\b");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("(?i)\\bparagrafo\\s*(\\d+)\\b");

    private record RetrievedChunk(
            int chunkId,
            String fileName,
            Integer page,
            String text,
            Double confidence
    ) {
    }

    @Transactional
    public AssessmentResult generateAssessment(Long macchinarioId) {
        log.info("Generating assessment for macchinario ID: {}", macchinarioId);

        Macchinario macchinario = macchinarioRepository.findById(macchinarioId)
                .orElseThrow(() -> new RuntimeException("Macchinario non trovato con ID: " + macchinarioId));

        return generateAssessment(macchinario);
    }

    @Transactional
    public AssessmentResult generateAssessment(Macchinario macchinario) {
        log.info("Generating assessment for macchinario: {}", macchinario.getNome());

        // Costruisci la query per la ricerca RAG
        String ragQuery = buildRagQuery(macchinario);

        // Cerca documenti rilevanti
        List<Document> relevantDocs = ragService.searchRelevantDocuments(ragQuery, EXPLAINABILITY_TOP_K);
        List<RetrievedChunk> retrievedChunks = toRetrievedChunks(relevantDocs);
        String context = buildExplainabilityContext(retrievedChunks);

        ChatClient chatClient = chatClientBuilder.build();

        // 1) Explainable output (JSON + citazioni via chunkId)
        String userPrompt = buildExplainableAssessmentPrompt(macchinario, context);
        String response = chatClient.prompt()
                .system(ASSESSMENT_SYSTEM_PROMPT_EXPLAINABLE)
                .user(userPrompt)
                .call()
                .content();

        Optional<AssessmentResult> explainable = parseExplainableAssessmentResponse(response, macchinario, retrievedChunks);

        // 2) Fallback legacy (solo se l'output non è JSON valido)
        AssessmentResult assessment = explainable.orElseGet(() -> {
            log.warn("Explainable JSON parsing failed, generating legacy assessment (no citations).");
            String legacyContext = ragService.buildContextFromDocuments(relevantDocs);
            List<String> documentSources = ragService.getDocumentSources(relevantDocs);
            String legacyPrompt = buildAssessmentPrompt(macchinario, legacyContext);

            String legacyResponse = chatClient.prompt()
                    .system(ASSESSMENT_SYSTEM_PROMPT)
                    .user(legacyPrompt)
                    .call()
                    .content();

            return parseAssessmentResponse(legacyResponse, macchinario, documentSources);
        });

        // Salva l'assessment
        assessment = assessmentResultRepository.save(assessment);

        log.info("Assessment generated successfully for macchinario: {}", macchinario.getNome());
        return assessment;
    }

    private String buildRagQuery(Macchinario macchinario) {
        StringBuilder query = new StringBuilder();
        query.append("Requisiti di sicurezza e conformità per ");
        query.append(macchinario.getCategoria() != null ? macchinario.getCategoria() + " " : "");
        query.append(macchinario.getNome());

        if (macchinario.getDescrizione() != null) {
            query.append(". ").append(macchinario.getDescrizione());
        }

        query.append(" Regolamento Macchine sicurezza requisiti essenziali marcatura CE");

        return query.toString();
    }

    private String buildAssessmentPrompt(Macchinario macchinario, String context) {
        return String.format("""
                Analizza il seguente macchinario e fornisci un assessment di conformità basandoti sui documenti di riferimento.
                
                === INFORMAZIONI MACCHINARIO ===
                Nome: %s
                Produttore: %s
                Modello: %s
                Numero di Serie: %s
                Anno di Produzione: %s
                Categoria: %s
                Descrizione: %s
                Specifiche Tecniche: %s
                
                %s
                
                Fornisci l'assessment completo nel seguente formato:
                
                ## RIEPILOGO CONFORMITÀ
                [Inserisci qui il riepilogo]
                
                ## NON CONFORMITÀ RILEVATE
                [Elenca le non conformità identificate]
                
                ## RACCOMANDAZIONI
                [Elenca le raccomandazioni per ogni non conformità]
                
                ## LIVELLO DI RISCHIO
                [BASSO/MEDIO/ALTO/CRITICO]
                
                ## PUNTEGGIO DI CONFORMITÀ
                [Numero da 0 a 100]
                """,
                macchinario.getNome(),
                macchinario.getProduttore() != null ? macchinario.getProduttore() : "Non specificato",
                macchinario.getModello() != null ? macchinario.getModello() : "Non specificato",
                macchinario.getNumeroSerie() != null ? macchinario.getNumeroSerie() : "Non specificato",
                macchinario.getAnnoProduzione() != null ? macchinario.getAnnoProduzione().toString() : "Non specificato",
                macchinario.getCategoria() != null ? macchinario.getCategoria() : "Non specificata",
                macchinario.getDescrizione() != null ? macchinario.getDescrizione() : "Non fornita",
                macchinario.getSpecificheTecniche() != null ? macchinario.getSpecificheTecniche() : "Non fornite",
                context
        );
    }

    private String buildExplainableAssessmentPrompt(Macchinario macchinario, String context) {
        return String.format("""
                Analizza il seguente macchinario e fornisci un assessment di conformità basandoti esclusivamente sui chunk normativi forniti.

                === INFORMAZIONI MACCHINARIO ===
                Nome: %s
                Produttore: %s
                Modello: %s
                Numero di Serie: %s
                Anno di Produzione: %s
                Categoria: %s
                Descrizione: %s
                Specifiche Tecniche: %s

                %s

                Restituisci SOLO un JSON con questo schema:
                {
                  "riepilogoConformita": "string",
                  "nonConformitaRilevate": [{"text":"string","chunkIds":[1,2]}],
                  "raccomandazioni": [{"text":"string","chunkIds":[1,2]}],
                  "livelloRischio": "BASSO|MEDIO|ALTO|CRITICO",
                  "punteggioConformita": 0
                }

                Regole:
                - "chunkIds" deve contenere SOLO numeri presenti nell'elenco chunk (CHUNK 1..N).
                - Ogni punto dovrebbe avere almeno 1 chunkId se possibile.
                - Non includere il testo dei chunk nel JSON: solo gli ID.
                """,
                macchinario.getNome(),
                macchinario.getProduttore() != null ? macchinario.getProduttore() : "Non specificato",
                macchinario.getModello() != null ? macchinario.getModello() : "Non specificato",
                macchinario.getNumeroSerie() != null ? macchinario.getNumeroSerie() : "Non specificato",
                macchinario.getAnnoProduzione() != null ? macchinario.getAnnoProduzione().toString() : "Non specificato",
                macchinario.getCategoria() != null ? macchinario.getCategoria() : "Non specificata",
                macchinario.getDescrizione() != null ? macchinario.getDescrizione() : "Non fornita",
                macchinario.getSpecificheTecniche() != null ? macchinario.getSpecificheTecniche() : "Non fornite",
                context
        );
    }

    private Optional<AssessmentResult> parseExplainableAssessmentResponse(String response, Macchinario macchinario, List<RetrievedChunk> retrievedChunks) {
        JsonNode root;
        try {
            root = objectMapper.readTree(extractFirstJsonObject(response));
        } catch (Exception e) {
            log.warn("Unable to parse explainable assessment JSON: {}", e.getMessage());
            return Optional.empty();
        }

        String riepilogoConformita = safe(textValue(root, "riepilogoConformita"));
        String livelloRischio = textValue(root, "livelloRischio");
        Integer punteggio = intValue(root.get("punteggioConformita")).orElse(null);

        List<ExplainableFindingDraft> nonConformitaDrafts = parseFindingDrafts(root.get("nonConformitaRilevate"));
        List<ExplainableFindingDraft> raccomandazioniDrafts = parseFindingDrafts(root.get("raccomandazioni"));

        Map<Integer, RetrievedChunk> chunkById = retrievedChunks.stream()
                .collect(Collectors.toMap(RetrievedChunk::chunkId, c -> c));

        List<ExplainableFinding> nonConformita = toExplainableFindings(nonConformitaDrafts, chunkById);
        List<ExplainableFinding> raccomandazioni = toExplainableFindings(raccomandazioniDrafts, chunkById);

        String nonConformitaJson;
        String raccomandazioniJson;
        try {
            nonConformitaJson = objectMapper.writeValueAsString(nonConformita);
            raccomandazioniJson = objectMapper.writeValueAsString(raccomandazioni);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize explainability payload: {}", e.getMessage());
            return Optional.empty();
        }

        AssessmentResult assessment = new AssessmentResult();
        assessment.setMacchinario(macchinario);
        assessment.setRisultato(response);
        assessment.setRiepilogoConformita(riepilogoConformita);
        assessment.setNonConformitaRilevate(nonConformitaJson);
        assessment.setRaccomandazioni(raccomandazioniJson);
        assessment.setLivelloRischio(normalizeRiskLevel(livelloRischio));
        assessment.setPunteggioConformita(normalizeScore(punteggio));

        Set<String> usedDocuments = new TreeSet<>();

        usedDocuments.addAll(nonConformita.stream()
                .flatMap(f -> f.sources().stream())
                .map(ExplainableSource::fileName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet()));

        usedDocuments.addAll(raccomandazioni.stream()
                .flatMap(f -> f.sources().stream())
                .map(ExplainableSource::fileName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet()));

        if (usedDocuments.isEmpty()) {
            usedDocuments.addAll(retrievedChunks.stream()
                    .map(RetrievedChunk::fileName)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isBlank())
                    .collect(Collectors.toSet()));
        }

        assessment.setDocumentiUtilizzati(String.join(", ", usedDocuments));

        return Optional.of(assessment);
    }

    private AssessmentResult parseAssessmentResponse(String response, Macchinario macchinario, List<String> documentSources) {
        AssessmentResult assessment = new AssessmentResult();
        assessment.setMacchinario(macchinario);
        assessment.setRisultato(response);

        // Estrai le sezioni dalla risposta
        assessment.setRiepilogoConformita(extractSection(response, "RIEPILOGO CONFORMITÀ"));
        assessment.setNonConformitaRilevate(extractSection(response, "NON CONFORMITÀ RILEVATE"));
        assessment.setRaccomandazioni(extractSection(response, "RACCOMANDAZIONI"));

        // Estrai livello di rischio
        String livelloRischio = extractSection(response, "LIVELLO DI RISCHIO").trim().toUpperCase();
        if (livelloRischio.contains("CRITICO")) {
            assessment.setLivelloRischio("CRITICO");
        } else if (livelloRischio.contains("ALTO")) {
            assessment.setLivelloRischio("ALTO");
        } else if (livelloRischio.contains("MEDIO")) {
            assessment.setLivelloRischio("MEDIO");
        } else {
            assessment.setLivelloRischio("BASSO");
        }

        // Estrai punteggio
        String punteggioStr = extractSection(response, "PUNTEGGIO DI CONFORMITÀ");
        try {
            int punteggio = Integer.parseInt(punteggioStr.replaceAll("[^0-9]", ""));
            assessment.setPunteggioConformita(Math.min(100, Math.max(0, punteggio)));
        } catch (NumberFormatException e) {
            assessment.setPunteggioConformita(50); // Default
        }

        // Documenti utilizzati
        assessment.setDocumentiUtilizzati(String.join(", ", documentSources));

        return assessment;
    }

    private List<ExplainableFindingDraft> parseFindingDrafts(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();

        List<ExplainableFindingDraft> drafts = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) continue;

            if (item.isTextual()) {
                drafts.add(new ExplainableFindingDraft(item.asText(), List.of()));
                continue;
            }

            if (!item.isObject()) continue;

            String text = item.path("text").asText(null);
            if (text == null || text.isBlank()) {
                text = item.path("testo").asText("");
            }

            List<Integer> chunkIds = parseChunkIds(item.get("chunkIds"));
            drafts.add(new ExplainableFindingDraft(text, chunkIds));
        }

        return drafts;
    }

    private List<Integer> parseChunkIds(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();

        Set<Integer> ids = new java.util.LinkedHashSet<>();
        for (JsonNode item : node) {
            intValue(item).ifPresent(ids::add);
        }

        return List.copyOf(ids);
    }

    private Optional<Integer> intValue(JsonNode node) {
        if (node == null || node.isNull()) return Optional.empty();
        if (node.isInt() || node.isLong()) return Optional.of(node.asInt());
        if (node.isNumber()) return Optional.of((int) Math.round(node.asDouble()));

        if (node.isTextual()) {
            String digits = node.asText().replaceAll("[^0-9-]", "");
            if (digits.isBlank()) return Optional.empty();
            try {
                return Optional.of(Integer.parseInt(digits));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private String textValue(JsonNode root, String fieldName) {
        if (root == null || fieldName == null) return null;
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) return null;
        return node.asText(null);
    }

    private List<RetrievedChunk> toRetrievedChunks(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return List.of();

        return java.util.stream.IntStream.range(0, documents.size())
                .mapToObj(i -> {
                    Document doc = documents.get(i);
                    Map<String, Object> metadata = doc.getMetadata();
                    String fileName = Optional.ofNullable(metadata.get("fileName"))
                            .or(() -> Optional.ofNullable(metadata.get("file_name")))
                            .or(() -> Optional.ofNullable(metadata.get("filename")))
                            .map(Object::toString)
                            .orElse("Documento");

                    Integer page = parseInteger(metadata.get("page"))
                            .or(() -> parseInteger(metadata.get("page_number")))
                            .or(() -> parseInteger(metadata.get("pageNumber")))
                            .orElse(null);

                    Double confidence = doc.getScore();
                    if (confidence == null) {
                        confidence = rankBasedConfidence(i);
                    }

                    return new RetrievedChunk(i + 1, fileName, page, doc.getText(), confidence);
                })
                .toList();
    }

    private String buildExplainabilityContext(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "=== CHUNKS NORMATIVI RECUPERATI (RAG) ===\n\nNessun documento di riferimento trovato nel database.\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== CHUNKS NORMATIVI RECUPERATI (RAG) ===\n");
        sb.append("Usa SOLO questi chunk come fonti. Per citare un chunk usa il suo chunkId (numero).\n\n");

        for (RetrievedChunk chunk : chunks) {
            sb.append("[CHUNK ").append(chunk.chunkId()).append("] ");
            sb.append("Fonte: ").append(chunk.fileName());
            if (chunk.page() != null) {
                sb.append(" | Pagina: ").append(chunk.page());
            }
            if (chunk.confidence() != null) {
                sb.append(" | Pertinenza: ").append(String.format(Locale.ROOT, "%.2f", chunk.confidence()));
            }
            sb.append("\n");
            sb.append(chunk.text()).append("\n\n");
        }

        return sb.toString();
    }

    private List<ExplainableFinding> toExplainableFindings(List<ExplainableFindingDraft> drafts, Map<Integer, RetrievedChunk> chunkById) {
        if (drafts == null || drafts.isEmpty()) return List.of();

        return drafts.stream()
                .filter(Objects::nonNull)
                .map(draft -> {
                    List<Integer> chunkIds = draft.chunkIds() != null ? draft.chunkIds() : List.of();
                    List<ExplainableSource> sources = chunkIds.stream()
                            .filter(Objects::nonNull)
                            .distinct()
                            .map(chunkById::get)
                            .filter(Objects::nonNull)
                            .map(this::toExplainableSource)
                            .toList();

                    return new ExplainableFinding(safe(draft.text()), sources);
                })
                .toList();
    }

    private ExplainableSource toExplainableSource(RetrievedChunk chunk) {
        String reference = buildReference(chunk.fileName(), chunk.page(), chunk.text());
        return new ExplainableSource(reference, chunk.fileName(), chunk.page(), chunk.text(), chunk.confidence());
    }

    private String buildReference(String fileName, Integer page, String chunkText) {
        String base = (fileName == null || fileName.isBlank()) ? "Documento" : fileName.trim();

        String articolo = firstMatch(ARTICLE_PATTERN, chunkText);
        String paragrafo = firstMatch(PARAGRAPH_PATTERN, chunkText);
        String comma = firstMatch(COMMA_PATTERN, chunkText);

        StringBuilder sb = new StringBuilder(base);
        if (articolo != null) {
            sb.append(", Articolo ").append(articolo);
        }
        if (paragrafo != null) {
            sb.append(", Paragrafo ").append(paragrafo);
        }
        if (comma != null) {
            sb.append(", Comma ").append(comma);
        }
        if (page != null) {
            sb.append(", Pag. ").append(page);
        }

        return sb.toString();
    }

    private String firstMatch(Pattern pattern, String input) {
        if (input == null || input.isBlank()) return null;
        Matcher matcher = pattern.matcher(input);
        if (!matcher.find()) return null;
        String value = matcher.group(1);
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private Optional<Integer> parseInteger(Object value) {
        if (value == null) return Optional.empty();
        if (value instanceof Number number) return Optional.of(number.intValue());

        String digits = value.toString().replaceAll("[^0-9-]", "");
        if (digits.isBlank()) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(digits));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Double rankBasedConfidence(int rankIndex) {
        double value = 0.9 - (rankIndex * 0.05);
        return Math.max(0.5, Math.min(0.95, value));
    }

    private String extractFirstJsonObject(String raw) {
        if (raw == null) return "{}";
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return "{}";
        return raw.substring(start, end + 1).trim();
    }

    private String safe(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }

    private String normalizeRiskLevel(String value) {
        if (value == null) return "MEDIO";
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("CRITICO")) return "CRITICO";
        if (normalized.contains("ALTO")) return "ALTO";
        if (normalized.contains("MEDIO")) return "MEDIO";
        if (normalized.contains("BASSO")) return "BASSO";
        return "MEDIO";
    }

    private Integer normalizeScore(Integer value) {
        if (value == null) return 50;
        return Math.min(100, Math.max(0, value));
    }

    private String extractSection(String text, String sectionName) {
        String pattern = "## " + sectionName;
        int startIndex = text.indexOf(pattern);
        if (startIndex == -1) {
            pattern = "**" + sectionName + "**";
            startIndex = text.indexOf(pattern);
        }
        if (startIndex == -1) {
            return "";
        }

        startIndex = text.indexOf("\n", startIndex);
        if (startIndex == -1) return "";

        int endIndex = text.indexOf("\n## ", startIndex + 1);
        if (endIndex == -1) {
            endIndex = text.indexOf("\n**", startIndex + 1);
        }
        if (endIndex == -1) {
            endIndex = text.length();
        }

        return text.substring(startIndex, endIndex).trim();
    }

    public List<AssessmentResult> getAssessmentsByMacchinario(Long macchinarioId) {
        return assessmentResultRepository.findByMacchinarioIdOrderByDataAssessmentDesc(macchinarioId);
    }

    public AssessmentResult getAssessment(Long id) {
        return assessmentResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment non trovato con ID: " + id));
    }

    public List<AssessmentResult> getAllAssessments() {
        return assessmentResultRepository.findAllByOrderByDataAssessmentDesc();
    }
}
