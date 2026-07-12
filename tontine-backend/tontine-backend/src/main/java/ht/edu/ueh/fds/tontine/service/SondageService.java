package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.SondageResponse;
import ht.edu.ueh.fds.tontine.entity.Sol;
import ht.edu.ueh.fds.tontine.entity.Sondage;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.entity.VoteSondage;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.SolRepository;
import ht.edu.ueh.fds.tontine.repository.SondageRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import ht.edu.ueh.fds.tontine.repository.VoteSondageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Regles metier des sondages / votes de groupe d'un Sol. */
@Service
@RequiredArgsConstructor
public class SondageService {

    private static final String SEP = "\n";

    private final SondageRepository sondageRepository;
    private final VoteSondageRepository voteRepository;
    private final MembreSolRepository membreSolRepository;
    private final SolRepository solRepository;
    private final UtilisateurRepository utilisateurRepository;

    /** Cree un sondage (reserve aux membres du Sol). */
    @Transactional
    public SondageResponse creer(String userId, String solId, String question, List<String> options) {
        if (!membreSolRepository.existsByUtilisateurIdAndSolIdAndStatutMembre(userId, solId, "ACTIF")) {
            throw new BusinessException("Vous n'etes pas membre de ce Sol.");
        }
        if (question == null || question.isBlank()) {
            throw new BusinessException("La question ne peut pas etre vide.");
        }
        List<String> propres = (options == null ? List.<String>of() : options).stream()
                .map(o -> o == null ? "" : o.trim())
                .filter(o -> !o.isEmpty())
                .toList();
        if (propres.size() < 2) {
            throw new BusinessException("Ajoutez au moins 2 options.");
        }
        if (propres.size() > 6) {
            throw new BusinessException("6 options au maximum.");
        }
        Utilisateur createur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        Sondage s = sondageRepository.save(Sondage.builder()
                .solId(solId)
                .createur(createur)
                .question(question.trim())
                .optionsTexte(String.join(SEP, propres))
                .build());
        return versReponse(s, userId);
    }

    /** Sondages d'un Sol (reserve aux membres). */
    @Transactional(readOnly = true)
    public List<SondageResponse> duSol(String userId, String solId) {
        if (!membreSolRepository.existsByUtilisateurIdAndSolIdAndStatutMembre(userId, solId, "ACTIF")) {
            throw new BusinessException("Vous n'etes pas membre de ce Sol.");
        }
        return sondageRepository.findBySolIdOrderByDateCreationDesc(solId).stream()
                .map(s -> versReponse(s, userId))
                .toList();
    }

    /** Enregistre (ou change) le vote de l'utilisateur pour une option. */
    @Transactional
    public SondageResponse voter(String userId, String sondageId, Integer optionIndex) {
        Sondage s = sondageRepository.findById(sondageId)
                .orElseThrow(() -> new BusinessException("Sondage introuvable."));
        if (!membreSolRepository.existsByUtilisateurIdAndSolIdAndStatutMembre(userId, s.getSolId(), "ACTIF")) {
            throw new BusinessException("Vous n'etes pas membre de ce Sol.");
        }
        if (!"OUVERT".equals(s.getStatut())) {
            throw new BusinessException("Ce sondage est clos.");
        }
        int nb = s.getOptionsTexte().split(SEP, -1).length;
        if (optionIndex == null || optionIndex < 0 || optionIndex >= nb) {
            throw new BusinessException("Option invalide.");
        }
        VoteSondage existant = voteRepository.findBySondageIdAndUtilisateurId(sondageId, userId).orElse(null);
        if (existant == null) {
            voteRepository.save(VoteSondage.builder()
                    .sondageId(sondageId).utilisateurId(userId).optionIndex(optionIndex).build());
        } else {
            existant.setOptionIndex(optionIndex);
            voteRepository.save(existant);
        }
        return versReponse(s, userId);
    }

    /** Clot un sondage (createur ou Manman sol). */
    @Transactional
    public SondageResponse cloturer(String userId, String sondageId) {
        Sondage s = sondageRepository.findById(sondageId)
                .orElseThrow(() -> new BusinessException("Sondage introuvable."));
        boolean estCreateur = s.getCreateur().getId().equals(userId);
        Sol sol = solRepository.findById(s.getSolId()).orElse(null);
        boolean estManman = sol != null && sol.getMamanSol() != null
                && sol.getMamanSol().getId().equals(userId);
        if (!estCreateur && !estManman) {
            throw new BusinessException("Seul le createur ou la Manman sol peut clore le sondage.");
        }
        s.setStatut("CLOS");
        sondageRepository.save(s);
        return versReponse(s, userId);
    }

    /** Construit la reponse (resultats + vote de l'utilisateur), en transaction. */
    private SondageResponse versReponse(Sondage s, String userId) {
        String[] opts = s.getOptionsTexte().split(SEP, -1);
        List<VoteSondage> votes = voteRepository.findBySondageId(s.getId());
        int total = votes.size();
        int[] compte = new int[opts.length];
        Integer monVote = null;
        for (VoteSondage v : votes) {
            int idx = v.getOptionIndex() == null ? -1 : v.getOptionIndex();
            if (idx >= 0 && idx < opts.length) {
                compte[idx]++;
            }
            if (v.getUtilisateurId().equals(userId)) {
                monVote = v.getOptionIndex();
            }
        }
        List<SondageResponse.OptionResultat> resultats = new ArrayList<>();
        for (int i = 0; i < opts.length; i++) {
            int pct = total == 0 ? 0 : (compte[i] * 100) / total;
            resultats.add(new SondageResponse.OptionResultat(i, opts[i], compte[i], pct));
        }
        boolean peutCloturer = "OUVERT".equals(s.getStatut())
                && s.getCreateur().getId().equals(userId);
        String createurNom = s.getCreateur().getPrenom() + " " + s.getCreateur().getNom();
        return new SondageResponse(s.getId(), s.getQuestion(), createurNom, s.getStatut(),
                s.getDateCreation(), total, monVote, peutCloturer, resultats);
    }
}
