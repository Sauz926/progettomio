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

    /**
     * Salva un nuovo macchinario nel database.
     * Chiamata dal controller macchinari nell'endpoint di creazione.
     *
     * @param macchinario entità da creare
     * @return entità persistita
     */
    @Transactional
    public Macchinario create(Macchinario macchinario) {
        log.info("Creating new macchinario: {}", macchinario.getNome());
        return macchinarioRepository.save(macchinario);
    }

    /**
     * Aggiorna un macchinario esistente con i nuovi dati ricevuti.
     * Chiamata dal controller macchinari nell'endpoint di update.
     *
     * @param id identificativo del record da aggiornare
     * @param macchinarioDetails nuovi valori
     * @return entità aggiornata
     */
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

    /**
     * Recupera un macchinario per ID o solleva errore se assente.
     * Chiamata da controller e da altri servizi (es. export PDF assessment).
     *
     * @param id identificativo macchinario
     * @return entità trovata
     */
    public Macchinario getById(Long id) {
        return macchinarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Macchinario non trovato con ID: " + id));
    }

    /**
     * Restituisce tutti i macchinari ordinati dal più recente.
     * Chiamata dal controller macchinari per la vista elenco.
     *
     * @return lista macchinari
     */
    public List<Macchinario> getAll() {
        return macchinarioRepository.findAllByOrderByDataCreazioneDesc();
    }

    /**
     * Cerca macchinari per nome parziale case-insensitive.
     * Chiamata dal controller macchinari endpoint search.
     *
     * @param nome frammento di nome
     * @return lista corrispondenze
     */
    public List<Macchinario> searchByName(String nome) {
        return macchinarioRepository.findByNomeContainingIgnoreCase(nome);
    }

    /**
     * Filtra macchinari per categoria.
     * Chiamata dal controller macchinari endpoint categoria.
     *
     * @param categoria categoria richiesta
     * @return macchinari appartenenti alla categoria
     */
    public List<Macchinario> getByCategoria(String categoria) {
        return macchinarioRepository.findByCategoriaIgnoreCase(categoria);
    }

    /**
     * Elimina un macchinario esistente.
     * Chiamata dal controller macchinari endpoint delete.
     *
     * @param id identificativo da cancellare
     */
    @Transactional
    public void delete(Long id) {
        Macchinario macchinario = getById(id);
        macchinarioRepository.delete(macchinario);
        log.info("Deleted macchinario: {}", macchinario.getNome());
    }
}
