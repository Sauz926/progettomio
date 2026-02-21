package it.unicas.spring.springai.service;

import it.unicas.spring.springai.model.DocumentEntity;
import it.unicas.spring.springai.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfIngestionService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;

    @Transactional
    public DocumentEntity uploadAndProcessPdf(MultipartFile file) throws IOException {
        log.info("Processing PDF file: {}", file.getOriginalFilename());

        // Salva il documento nel database
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setFileName(UUID.randomUUID().toString() + ".pdf");
        documentEntity.setOriginalFileName(file.getOriginalFilename());
        documentEntity.setFileSize(file.getSize());
        documentEntity.setContentType(file.getContentType());
        documentEntity.setFileContent(file.getBytes());
        documentEntity.setProcessed(false);

        documentEntity = documentRepository.save(documentEntity);

        // Processa il PDF per il vector store
        processAndStoreEmbeddings(documentEntity);

        return documentEntity;
    }

    @Transactional
    public void processAndStoreEmbeddings(DocumentEntity documentEntity) {
        log.info("Generating embeddings for document: {}", documentEntity.getOriginalFileName());

        try {
            // Crea una risorsa dal contenuto del file
            ByteArrayResource resource = new ByteArrayResource(documentEntity.getFileContent()) {
                @Override
                public String getFilename() {
                    return documentEntity.getOriginalFileName();
                }
            };

            // Leggi il PDF
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            List<Document> documents = pdfReader.get();

            // Splitter per dividere in chunks
            TextSplitter textSplitter = new TokenTextSplitter();
            List<Document> chunks = textSplitter.apply(documents);

            // Aggiungi metadata a ogni chunk (Document è immutabile: crea una copia arricchita)
            List<Document> enrichedChunks = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("documentId", documentEntity.getId());
                metadata.put("fileName", documentEntity.getOriginalFileName());
                metadata.put("source", "uploaded_pdf");
                metadata.put("chunkIndex", i + 1);

                Integer page = extractPageNumber(metadata);
                if (page != null) {
                    metadata.put("page", page);
                }

                enrichedChunks.add(chunk.mutate().metadata(metadata).build());
            }

            // Salva nel vector store
            log.info("Adding {} chunks to vector store for document: {}", enrichedChunks.size(), documentEntity.getOriginalFileName());
            vectorStore.add(enrichedChunks);

            log.info("Update document status to processed for: {}", documentEntity.getOriginalFileName());
            // Aggiorna lo stato del documento
            documentEntity.setProcessed(true);
            documentEntity.setChunkCount(enrichedChunks.size());
            documentRepository.save(documentEntity);

            log.info("Successfully processed {} chunks for document: {}",
                    enrichedChunks.size(), documentEntity.getOriginalFileName());

        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process PDF: " + e.getMessage(), e);
        }
    }

    private Integer extractPageNumber(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;

        Object page = metadata.get("page");
        if (page == null) page = metadata.get("page_number");
        if (page == null) page = metadata.get("pageNumber");
        if (page == null) page = metadata.get("page-number");

        if (page instanceof Number number) {
            return number.intValue();
        }

        if (page instanceof String str) {
            String digits = str.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return null;
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    public List<DocumentEntity> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadDateDesc();
    }

    public DocumentEntity getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
    }

    @Transactional
    public void deleteDocument(Long id) {
        DocumentEntity document = getDocument(id);
        // TODO: Rimuovere anche gli embeddings dal vector store
        documentRepository.delete(document);
        log.info("Deleted document: {}", document.getOriginalFileName());
    }
}
