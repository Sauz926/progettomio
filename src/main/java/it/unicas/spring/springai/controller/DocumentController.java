package it.unicas.spring.springai.controller;

import it.unicas.spring.springai.model.DocumentEntity;
import it.unicas.spring.springai.service.PdfIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final PdfIngestionService pdfIngestionService;

    /**
     * Gestisce il caricamento di un PDF, valida formato/contenuto e delega l'ingestione al servizio RAG.
     * Chiamata da Spring MVC quando arriva una {@code POST /api/documents/upload}; internamente invoca
     * {@link PdfIngestionService#uploadAndProcessPdf(MultipartFile)}.
     *
     * @param file file PDF inviato dal client
     * @return payload JSON con metadati del documento o errore di validazione/elaborazione
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il file è vuoto"));
        }

        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        boolean isPdf = MediaType.APPLICATION_PDF_VALUE.equals(contentType) || originalName.endsWith(".pdf");
        if (!isPdf) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo file PDF sono accettati"));
        }

        try {
            DocumentEntity document = pdfIngestionService.uploadAndProcessPdf(file);
            //log.info("File uploaded and processed: {}", document.getOriginalFileName());
            Map<String, Object> response = new HashMap<>();
            response.put("id", document.getId());
            response.put("fileName", document.getOriginalFileName());
            response.put("fileSize", document.getFileSize());
            response.put("uploadDate", document.getUploadDate());
            response.put("processed", document.isProcessed());
            response.put("chunkCount", document.getChunkCount());
            response.put("message", "File caricato e processato con successo");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Errore durante il caricamento del file: " + e.getMessage()));
        }
    }

    /**
     * Restituisce la lista dei documenti caricati con i metadati principali.
     * Chiamata da Spring MVC tramite {@code GET /api/documents}; usa
     * {@link PdfIngestionService#getAllDocuments()} come sorgente dati.
     *
     * @return elenco documenti normalizzato per la risposta REST
     */
    @GetMapping
    public ResponseEntity<?> getAllDocuments(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        List<DocumentEntity> documents = pdfIngestionService.getAllDocuments();

        List<Map<String, Object>> response = documents.stream().map(doc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doc.getId());
            map.put("fileName", doc.getOriginalFileName());
            map.put("fileSize", doc.getFileSize());
            map.put("uploadDate", doc.getUploadDate());
            map.put("processed", doc.isProcessed());
            map.put("chunkCount", doc.getChunkCount());
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Recupera i dettagli di un singolo documento.
     * Chiamata da Spring MVC tramite {@code GET /api/documents/{id}}; delega a
     * {@link PdfIngestionService#getDocument(Long)}.
     *
     * @param id identificativo del documento
     * @return metadati del documento oppure 404 se non esiste
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        try {
            DocumentEntity document = pdfIngestionService.getDocument(id);

            Map<String, Object> response = new HashMap<>();
            response.put("id", document.getId());
            response.put("fileName", document.getOriginalFileName());
            response.put("fileSize", document.getFileSize());
            response.put("uploadDate", document.getUploadDate());
            response.put("processed", document.isProcessed());
            response.put("chunkCount", document.getChunkCount());
            response.put("description", document.getDescription());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Fornisce il contenuto binario del PDF per il download.
     * Chiamata da Spring MVC tramite {@code GET /api/documents/{id}/download}; recupera il documento
     * con {@link PdfIngestionService#getDocument(Long)} e imposta gli header HTTP di allegato.
     *
     * @param id identificativo del documento
     * @return bytes PDF con header di download oppure 404
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        try {
            DocumentEntity document = pdfIngestionService.getDocument(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", document.getOriginalFileName());

            return new ResponseEntity<>(document.getFileContent(), headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Elimina un documento già caricato.
     * Chiamata da Spring MVC tramite {@code DELETE /api/documents/{id}}; invoca
     * {@link PdfIngestionService#deleteDocument(Long)} per la cancellazione.
     *
     * @param id identificativo del documento
     * @return conferma operazione o 404 se il documento non esiste
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return forbidden();
        }

        try {
            pdfIngestionService.deleteDocument(id);
            return ResponseEntity.ok(Map.of("message", "Documento eliminato con successo"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Accesso riservato agli admin"));
    }
}
