package it.unicas.spring.springai.repository;

import it.unicas.spring.springai.model.Macchinario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MacchinarioRepository extends JpaRepository<Macchinario, Long> {

    List<Macchinario> findByNomeContainingIgnoreCase(String nome);

    List<Macchinario> findByCategoriaIgnoreCase(String categoria);

    List<Macchinario> findByProduttoreIgnoreCase(String produttore);

    List<Macchinario> findAllByOrderByDataCreazioneDesc();
}
