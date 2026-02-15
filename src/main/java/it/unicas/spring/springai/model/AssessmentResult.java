package it.unicas.spring.springai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "macchinario_id", nullable = false)
    private Macchinario macchinario;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String risultato;

    @Column(columnDefinition = "TEXT")
    private String riepilogoConformita;

    @Column(columnDefinition = "TEXT")
    private String nonConformitaRilevate;

    @Column(columnDefinition = "TEXT")
    private String raccomandazioni;

    @Column
    private String livelloRischio; // BASSO, MEDIO, ALTO, CRITICO

    @Column
    private Integer punteggioConformita; // 0-100

    @Column(columnDefinition = "TEXT")
    private String documentiUtilizzati;

    @Column(nullable = false)
    private LocalDateTime dataAssessment;

    @Column
    private String versione;

    @PrePersist
    protected void onCreate() {
        dataAssessment = LocalDateTime.now();
        versione = "1.0";
    }
}
