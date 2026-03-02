package it.unicas.spring.springai.controller;

import it.unicas.spring.springai.model.AssessmentResult;
import it.unicas.spring.springai.model.Macchinario;
import it.unicas.spring.springai.service.AssessmentPdfService;
import it.unicas.spring.springai.service.AssessmentService;
import it.unicas.spring.springai.service.MachineManualExtractionService;
import it.unicas.spring.springai.service.MacchinarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    private final AssessmentPdfService assessmentPdfService;

    /**
     * Crea un nuovo macchinario validando i campi minimi richiesti.
     * Chiamata da Spring MVC tramite {@code POST /api/macchinari}; delega il salvataggio a
     * {@link MacchinarioService#create(Macchinario)}.
     *
     * @param macchinario payload inviato dal client
     * @return macchinario creato oppure errore di validazione
     */
    @PostMapping
    public ResponseEntity<?> createMacchinario(@RequestBody Macchinario macchinario) {
        if (macchinario == null || macchinario.getNome() == null || macchinario.getNome().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il nome è obbligatorio"));
        }

        macchinario.setNome(macchinario.getNome().trim());
        Macchinario created = macchinarioService.create(macchinario);
        return ResponseEntity.ok(created);
    }

    /**
     * Restituisce tutti i macchinari ordinati per data creazione.
     * Chiamata da Spring MVC tramite {@code GET /api/macchinari}; usa
     * {@link MacchinarioService#getAll()}.
     *
     * @return elenco dei macchinari disponibili
     */
    @GetMapping
    public ResponseEntity<List<Macchinario>> getAllMacchinari() {
        return ResponseEntity.ok(macchinarioService.getAll());
    }

    /**
     * Recupera il dettaglio di un macchinario per ID.
     * Chiamata da Spring MVC tramite {@code GET /api/macchinari/{id}}; invoca
     * {@link MacchinarioService#getById(Long)}.
     *
     * @param id identificativo macchinario
     * @return macchinario richiesto o 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Macchinario> getMacchinario(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(macchinarioService.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Aggiorna i dati anagrafici/tecnici di un macchinario esistente.
     * Chiamata da Spring MVC tramite {@code PUT /api/macchinari/{id}}; delega a
     * {@link MacchinarioService#update(Long, Macchinario)}.
     *
     * @param id identificativo macchinario
     * @param macchinario nuovo stato richiesto dal client
     * @return macchinario aggiornato o errore di validazione/404
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMacchinario(@PathVariable Long id, @RequestBody Macchinario macchinario) {
        if (macchinario == null || macchinario.getNome() == null || macchinario.getNome().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il nome è obbligatorio"));
        }

        macchinario.setNome(macchinario.getNome().trim());
        try {
            return ResponseEntity.ok(macchinarioService.update(id, macchinario));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Elimina un macchinario esistente.
     * Chiamata da Spring MVC tramite {@code DELETE /api/macchinari/{id}}; usa
     * {@link MacchinarioService#delete(Long)}.
     *
     * @param id identificativo macchinario
     * @return conferma cancellazione o 404
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMacchinario(@PathVariable Long id) {
        try {
            macchinarioService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Macchinario eliminato con successo"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Esegue ricerca testuale per nome macchinario.
     * Chiamata da Spring MVC tramite {@code GET /api/macchinari/search}; invoca
     * {@link MacchinarioService#searchByName(String)}.
     *
     * @param nome filtro nome parziale
     * @return lista macchinari corrispondenti
     */
    @GetMapping("/search")
    public ResponseEntity<List<Macchinario>> searchMacchinari(@RequestParam String nome) {
        return ResponseEntity.ok(macchinarioService.searchByName(nome));
    }

    /**
     * Restituisce i macchinari appartenenti a una categoria.
     * Chiamata da Spring MVC tramite {@code GET /api/macchinari/categoria/{categoria}}; delega a
     * {@link MacchinarioService#getByCategoria(String)}.
     *
     * @param categoria categoria richiesta
     * @return macchinari della categoria indicata
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<Macchinario>> getByCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(macchinarioService.getByCategoria(categoria));
    }

    /**
     * Estrae automaticamente i dati del macchinario da un manuale PDF caricato.
     * Chiamata da Spring MVC tramite {@code POST /api/macchinari/manual/extract}; valida il file e poi usa
     * {@link MachineManualExtractionService#extractFromManual(MultipartFile)}.
     *
     * @param file manuale PDF da analizzare
     * @return struttura dati estratta o errore di validazione/elaborazione
     */
    @PostMapping("/manual/extract")
    public ResponseEntity<?> extractFromManual(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il file è vuoto"));
        }

        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        boolean looksLikePdf = originalName.toLowerCase().endsWith(".pdf");

        if (!looksLikePdf && !MediaType.APPLICATION_PDF_VALUE.equals(contentType)) {
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

    /**
     * Genera un nuovo assessment di conformità per il macchinario indicato.
     * Chiamata da Spring MVC tramite {@code POST /api/macchinari/{id}/assessment}; invoca
     * {@link AssessmentService#generateAssessment(Long)}.
     *
     * @param id identificativo macchinario
     * @return assessment appena generato o errore di business
     */
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

    /**
     * Elenca gli assessment storici di un macchinario.
     * Chiamata da Spring MVC tramite {@code GET /api/macchinari/{id}/assessments}; usa
     * {@link AssessmentService#getAssessmentsByMacchinario(Long)}.
     *
     * @param id identificativo macchinario
     * @return lista assessment ordinati dal più recente
     */
    @GetMapping("/{id}/assessments")
    public ResponseEntity<List<Map<String, Object>>> getAssessments(@PathVariable Long id) {
        List<AssessmentResult> assessments = assessmentService.getAssessmentsByMacchinario(id);
        List<Map<String, Object>> response = assessments.stream()
                .map(this::mapAssessmentToResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Genera e scarica il PDF dell'ultimo assessment disponibile per il macchinario.
     * Chiamata da Spring MVC tramite {@code GET /api/macchinari/{id}/assessments/latest/pdf}; coordina
     * {@link MacchinarioService#getById(Long)}, {@link AssessmentService#getAssessmentsByMacchinario(Long)}
     * e {@link AssessmentPdfService#generateAssessmentPdf(Macchinario, AssessmentResult)}.
     *
     * @param id identificativo macchinario
     * @return stream PDF in download oppure errore 404/500
     */
    @GetMapping("/{id}/assessments/latest/pdf")
    public ResponseEntity<?> downloadLatestAssessmentPdf(@PathVariable Long id) {
        Macchinario macchinario;
        try {
            macchinario = macchinarioService.getById(id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }

        List<AssessmentResult> assessments = assessmentService.getAssessmentsByMacchinario(id);
        if (assessments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Nessun assessment disponibile per questo macchinario"));
        }

        AssessmentResult latest = assessments.get(0);

        try {
            byte[] pdf = assessmentPdfService.generateAssessmentPdf(macchinario, latest);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setCacheControl(CacheControl.noStore());
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(assessmentPdfService.buildDownloadFilename(macchinario, latest), StandardCharsets.UTF_8)
                            .build()
            );

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Error generating assessment PDF for macchinario {} (assessment {})", id, latest.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Converte un assessment nel formato risposta usato dai controller REST.
     * Chiamata internamente da {@link #generateAssessment(Long)} e {@link #getAssessments(Long)}.
     *
     * @param assessment assessment da serializzare
     * @return mappa campi da restituire al client
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
