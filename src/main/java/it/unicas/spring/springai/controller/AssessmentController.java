package it.unicas.spring.springai.controller;

import it.unicas.spring.springai.model.AssessmentResult;
import it.unicas.spring.springai.service.AssessmentService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllAssessments() {
        List<AssessmentResult> assessments = assessmentService.getAllAssessments();
        List<Map<String, Object>> response = assessments.stream()
                .map(this::mapAssessmentToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAssessment(@PathVariable Long id) {
        try {
            AssessmentResult assessment = assessmentService.getAssessment(id);
            return ResponseEntity.ok(mapAssessmentToResponse(assessment));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

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
