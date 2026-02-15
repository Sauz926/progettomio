package it.unicas.spring.springai.repository;

import it.unicas.spring.springai.model.AssessmentResult;
import it.unicas.spring.springai.model.Macchinario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long> {

    List<AssessmentResult> findByMacchinarioOrderByDataAssessmentDesc(Macchinario macchinario);

    List<AssessmentResult> findByMacchinarioIdOrderByDataAssessmentDesc(Long macchinarioId);

    List<AssessmentResult> findByLivelloRischio(String livelloRischio);

    List<AssessmentResult> findAllByOrderByDataAssessmentDesc();
}
