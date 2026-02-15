package it.unicas.spring.springai.service;

import it.unicas.spring.springai.model.Macchinario;
import it.unicas.spring.springai.repository.MacchinarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MacchinarioService {

    private final MacchinarioRepository macchinarioRepository;

    @Transactional
    public Macchinario create(Macchinario macchinario) {
        log.info("Creating new macchinario: {}", macchinario.getNome());
        return macchinarioRepository.save(macchinario);
    }

    @Transactional
    public Macchinario update(Long id, Macchinario macchinarioDetails) {
        Macchinario macchinario = getById(id);

        macchinario.setNome(macchinarioDetails.getNome());
        macchinario.setDescrizione(macchinarioDetails.getDescrizione());
        macchinario.setProduttore(macchinarioDetails.getProduttore());
        macchinario.setModello(macchinarioDetails.getModello());
        macchinario.setNumeroSerie(macchinarioDetails.getNumeroSerie());
        macchinario.setAnnoProduzione(macchinarioDetails.getAnnoProduzione());
        macchinario.setCategoria(macchinarioDetails.getCategoria());
        macchinario.setSpecificheTecniche(macchinarioDetails.getSpecificheTecniche());

        log.info("Updated macchinario: {}", macchinario.getNome());
        return macchinarioRepository.save(macchinario);
    }

    public Macchinario getById(Long id) {
        return macchinarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Macchinario non trovato con ID: " + id));
    }

    public List<Macchinario> getAll() {
        return macchinarioRepository.findAllByOrderByDataCreazioneDesc();
    }

    public List<Macchinario> searchByName(String nome) {
        return macchinarioRepository.findByNomeContainingIgnoreCase(nome);
    }

    public List<Macchinario> getByCategoria(String categoria) {
        return macchinarioRepository.findByCategoriaIgnoreCase(categoria);
    }

    @Transactional
    public void delete(Long id) {
        Macchinario macchinario = getById(id);
        macchinarioRepository.delete(macchinario);
        log.info("Deleted macchinario: {}", macchinario.getNome());
    }
}
