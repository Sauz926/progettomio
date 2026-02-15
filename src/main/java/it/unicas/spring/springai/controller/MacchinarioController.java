package it.unicas.spring.springai.controller;

import it.unicas.spring.springai.model.AssessmentResult;
import it.unicas.spring.springai.model.Macchinario;
import it.unicas.spring.springai.service.AssessmentService;
import it.unicas.spring.springai.service.MachineManualExtractionService;
import it.unicas.spring.springai.service.MacchinarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/macchinari")
@RequiredArgsConstructor
@Slf4j
public class MacchinarioController {

    private final MacchinarioService macchinarioService;
    private final AssessmentService assessmentService;
    private final MachineManualExtractionService machineManualExtractionService;

    @PostMapping
    public ResponseEntity<Macchinario> createMacchinario(@RequestBody Macchinario macchinario) {
        Macchinario created = macchinarioService.create(macchinario);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<Macchinario>> getAllMacchinari() {
        return ResponseEntity.ok(macchinarioService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Macchinario> getMacchinario(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(macchinarioService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Macchinario> updateMacchinario(@PathVariable Long id, @RequestBody Macchinario macchinario) {
        try {
            return ResponseEntity.ok(macchinarioService.update(id, macchinario));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMacchinario(@PathVariable Long id) {
        try {
            macchinarioService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Macchinario eliminato con successo"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Macchinario>> searchMacchinari(@RequestParam String nome) {
        return ResponseEntity.ok(macchinarioService.searchByName(nome));
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Macchinario>> getByCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(macchinarioService.getByCategoria(categoria));
    }

    @PostMapping("/manual/extract")
    public ResponseEntity<?> extractFromManual(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il file è vuoto"));
        }

        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        boolean looksLikePdf = originalName.toLowerCase().endsWith(".pdf");

        if (contentType != null && !contentType.equals("application/pdf") && !looksLikePdf) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo file PDF sono accettati"));
        }

        try {
            return ResponseEntity.ok(machineManualExtractionService.extractFromManual(file));
        } catch (IOException e) {
            log.error("Error reading manual PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Errore durante la lettura del PDF: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error extracting machine data from manual: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Assessment endpoints

    @PostMapping("/{id}/assessment")
    public ResponseEntity<?> generateAssessment(@PathVariable Long id) {
        try {
            log.info("Generating assessment for macchinario ID: {}", id);
            AssessmentResult assessment = assessmentService.generateAssessment(id);
            return ResponseEntity.ok(mapAssessmentToResponse(assessment));
        } catch (RuntimeException e) {
            log.error("Error generating assessment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/assessments")
    public ResponseEntity<List<Map<String, Object>>> getAssessments(@PathVariable Long id) {
        List<AssessmentResult> assessments = assessmentService.getAssessmentsByMacchinario(id);
        List<Map<String, Object>> response = assessments.stream()
                .map(this::mapAssessmentToResponse)
                .toList();
        return ResponseEntity.ok(response);
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
