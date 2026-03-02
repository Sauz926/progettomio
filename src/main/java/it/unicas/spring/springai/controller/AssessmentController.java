package it.unicas.spring.springai.controller;

import it.unicas.spring.springai.dto.AssessmentChatRequest;
import it.unicas.spring.springai.dto.AssessmentChatResponse;
import it.unicas.spring.springai.model.AssessmentResult;
import it.unicas.spring.springai.service.AssessmentChatService;
import it.unicas.spring.springai.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AssessmentChatService assessmentChatService;

    /**
     * Espone l'elenco completo degli assessment salvati.
     * Chiamata da Spring MVC tramite {@code GET /api/assessments}; utilizza
     * {@link AssessmentService#getAllAssessments()} e converte ciascuna entità nel payload REST.
     *
     * @return lista di assessment in formato JSON
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllAssessments() {
        List<AssessmentResult> assessments = assessmentService.getAllAssessments();
        List<Map<String, Object>> response = assessments.stream()
                .map(this::mapAssessmentToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Restituisce il dettaglio di un assessment specifico.
     * Chiamata da Spring MVC tramite {@code GET /api/assessments/{id}}; delega a
     * {@link AssessmentService#getAssessment(Long)}.
     *
     * @param id identificativo assessment
     * @return assessment richiesto o 404 se non trovato
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAssessment(@PathVariable Long id) {
        try {
            AssessmentResult assessment = assessmentService.getAssessment(id);
            return ResponseEntity.ok(mapAssessmentToResponse(assessment));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Gestisce una domanda contestuale su un assessment già generato.
     * Chiamata da Spring MVC tramite {@code POST /api/assessments/{id}/chat}; invoca
     * {@link AssessmentChatService#chat(Long, AssessmentChatRequest)}.
     *
     * @param id identificativo assessment
     * @param request corpo con domanda e storia chat
     * @return risposta testuale contestualizzata o errore di validazione/elaborazione
     */
    @PostMapping("/{id}/chat")
    public ResponseEntity<?> chatOnAssessment(@PathVariable Long id, @RequestBody AssessmentChatRequest request) {
        try {
            String answer = assessmentChatService.chat(id, request);
            return ResponseEntity.ok(new AssessmentChatResponse(answer));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Errore durante la chat";
            HttpStatus status = message.toLowerCase().contains("non trovato") ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(Map.of("error", message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Errore durante la chat: " + e.getMessage()));
        }
    }

    /**
     * Converte un'entità {@link AssessmentResult} nel formato mappa restituito dall'API.
     * Chiamata internamente da {@link #getAllAssessments()} e {@link #getAssessment(Long)} per mantenere
     * coerente lo schema di risposta.
     *
     * @param assessment entità assessment persistita
     * @return mappa serializzabile con i campi esposti al client
     */
    private Map<String, Object> mapAssessmentToResponse(AssessmentResult assessment) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", assessment.getId());
        map.put("macchinarioId", assessment.getMacchinario().getId());
        map.put("macchinarioNome", assessment.getMacchinario().getNome());
        map.put("risultato", assessment.getRisultato());
        map.put("riepilogoConformita", assessment.getRiepilogoConformita());
        map.put("nonConformitaRilevate", assessment.getNonConformitaRilevate());
        map.put("raccomandazioni", assessment.getRaccomandazioni());
        map.put("livelloRischio", assessment.getLivelloRischio());
        map.put("punteggioConformita", assessment.getPunteggioConformita());
        map.put("documentiUtilizzati", assessment.getDocumentiUtilizzati());
        map.put("dataAssessment", assessment.getDataAssessment());
        map.put("versione", assessment.getVersione());
        return map;
    }
}
