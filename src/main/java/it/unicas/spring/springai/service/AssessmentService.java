package it.unicas.spring.springai.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final ChatClient.Builder chatClientBuilder;
    private final RagService ragService;
    private final MacchinarioRepository macchinarioRepository;
    private final AssessmentResultRepository assessmentResultRepository;

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
        List<Document> relevantDocs = ragService.searchRelevantDocuments(ragQuery, 10);
        String context = ragService.buildContextFromDocuments(relevantDocs);
        List<String> documentSources = ragService.getDocumentSources(relevantDocs);

        // Costruisci il prompt per l'assessment
        String userPrompt = buildAssessmentPrompt(macchinario, context);

        // Genera l'assessment usando il modello LLM
        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system(ASSESSMENT_SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();

        // Parsa la risposta e crea l'AssessmentResult
        AssessmentResult assessment = parseAssessmentResponse(response, macchinario, documentSources);

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
