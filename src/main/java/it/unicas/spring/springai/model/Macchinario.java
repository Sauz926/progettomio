package it.unicas.spring.springai.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "macchinari")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Macchinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descrizione;

    @Column
    private String produttore;

    @Column
    private String modello;

    @Column
    private String numeroSerie;

    @Column
    private Integer annoProduzione;

    @Column
    private String categoria;

    @Column(columnDefinition = "TEXT")
    private String specificheTecniche;

    @Column(nullable = false)
    private LocalDateTime dataCreazione;

    @Column
    private LocalDateTime dataUltimaModifica;

    @OneToMany(mappedBy = "macchinario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AssessmentResult> assessments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dataCreazione = LocalDateTime.now();
        dataUltimaModifica = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataUltimaModifica = LocalDateTime.now();
    }
}

