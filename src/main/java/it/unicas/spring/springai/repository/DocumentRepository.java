package it.unicas.spring.springai.repository;

import it.unicas.spring.springai.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByFileName(String fileName);

    List<DocumentEntity> findByProcessed(boolean processed);

    List<DocumentEntity> findAllByOrderByUploadDateDesc();

    boolean existsByOriginalFileName(String originalFileName);
}
