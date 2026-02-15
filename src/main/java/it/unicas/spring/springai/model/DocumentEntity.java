package it.unicas.spring.springai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_content", columnDefinition = "bytea", nullable = false)
    private byte[] fileContent;

    @Column(name = "testo_estratto", columnDefinition = "TEXT")
    private String testoEstratto;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "processed")
    private boolean processed;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "description")
    private String description;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }
}
