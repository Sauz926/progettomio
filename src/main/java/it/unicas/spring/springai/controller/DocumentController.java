package it.unicas.spring.springai.controller;

import it.unicas.spring.springai.model.DocumentEntity;
import it.unicas.spring.springai.service.PdfIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Il file è vuoto"));
        }

        if (!file.getContentType().equals("application/pdf")) {
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

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDocuments() {
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
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

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            pdfIngestionService.deleteDocument(id);
            return ResponseEntity.ok(Map.of("message", "Documento eliminato con successo"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
