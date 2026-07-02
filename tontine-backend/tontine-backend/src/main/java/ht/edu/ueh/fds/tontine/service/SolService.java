package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.entity.CaisseGarantie;
import ht.edu.ueh.fds.tontine.entity.MembreSol;
import ht.edu.ueh.fds.tontine.entity.Sol;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.CaisseGarantieRepository;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.SolRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

/**
 * Regles metier du cercle de tontine :
 * creation d'un Sol, adhesion via code d'invitation, desinscription.
 */
@Service
@RequiredArgsConstructor
public class SolService {

    private static final String ALPHABET_CODE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGUEUR_CODE = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SolRepository solRepository;
    private final MembreSolRepository membreSolRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CaisseGarantieRepository caisseGarantieRepository;

    /**
     * Cas « Creer un Sol » (Manman sol) :
     * cree le cercle, genere le code d'invitation unique, ouvre la caisse
     * de garantie et inscrit la creatrice comme premier membre.
     */
    @Transactional
    public Sol creerSol(String mamanSolId, Sol solACreer) {
        Utilisateur mamanSol = utilisateurRepository.findById(mamanSolId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable : " + mamanSolId));

        if (!"ACTIF".equals(mamanSol.getStatut())) {
            throw new BusinessException("Le compte doit etre actif pour creer un Sol.");
        }
        if (solACreer.getNombreMaxMembres() == null || solACreer.getNombreMaxMembres() < 2) {
            throw new BusinessException("Un Sol doit compter au moins 2 places.");
        }
        if (solACreer.getMontantCotisation() == null
                || solACreer.getMontantCotisation().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de la cotisation doit etre positif.");
        }

        solACreer.setMamanSol(mamanSol);
        solACreer.setStatut("OUVERT");
        solACreer.setCodeInvitation(genererCodeInvitationUnique());
        Sol sol = solRepository.save(solACreer);

        // La caisse de garantie du Sol (une seule par Sol), vide au depart.
        caisseGarantieRepository.save(CaisseGarantie.builder()
                .sol(sol)
                .solde(BigDecimal.ZERO)
                .build());

        // La Manman sol occupe la premiere place de la rotation.
        membreSolRepository.save(MembreSol.builder()
                .utilisateur(mamanSol)
                .sol(sol)
                .ordrePassage(1)
                .build());

        return sol;
    }

    /**
     * Cas « S'inscrire dans un Sol / Entrer dans une communaute » :
     * l'utilisateur rejoint une tontine grace au code partage.
     */
    @Transactional
    public MembreSol rejoindreParCode(String utilisateurId, String codeInvitation) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable : " + utilisateurId));
        Sol sol = solRepository.findByCodeInvitation(codeInvitation)
                .orElseThrow(() -> new BusinessException("Code d'invitation invalide."));

        if (!"OUVERT".equals(sol.getStatut())) {
            throw new BusinessException("Ce Sol n'accepte plus de nouveaux membres (cycle demarre ou termine).");
        }
        if (membreSolRepository.existsByUtilisateurIdAndSolId(utilisateurId, sol.getId())) {
            throw new BusinessException("Vous etes deja membre de ce Sol.");
        }
        long placesOccupees = membreSolRepository.countBySolId(sol.getId());
        if (placesOccupees >= sol.getNombreMaxMembres()) {
            throw new BusinessException("Ce Sol est complet.");
        }

        return membreSolRepository.save(MembreSol.builder()
                .utilisateur(utilisateur)
                .sol(sol)
                .ordrePassage((int) placesOccupees + 1)
                .build());
    }

    /**
     * Cas « Se desinscrire d'une communaute » :
     * rejete si le cycle est en cours (dette potentielle envers le groupe).
     */
    @Transactional
    public void seDesinscrire(String utilisateurId, String solId) {
        MembreSol membre = membreSolRepository.findByUtilisateurIdAndSolId(utilisateurId, solId)
                .orElseThrow(() -> new BusinessException("Vous n'etes pas membre de ce Sol."));
        Sol sol = membre.getSol();

        if ("EN_COURS".equals(sol.getStatut())) {
            throw new BusinessException(
                    "Impossible de quitter un Sol dont le cycle est en cours : vous avez un engagement envers le groupe.");
        }
        if ("DEFAILLANT".equals(membre.getStatutMembre())) {
            throw new BusinessException("Impossible de quitter le Sol avec une dette active.");
        }

        membre.setStatutMembre("PARTI");
        membreSolRepository.save(membre);
    }

    /** Demarre officiellement le cycle : plus aucune adhesion possible. */
    @Transactional
    public Sol demarrerCycle(String mamanSolId, String solId) {
        Sol sol = solRepository.findById(solId)
                .orElseThrow(() -> new BusinessException("Sol introuvable : " + solId));
        if (!sol.getMamanSol().getId().equals(mamanSolId)) {
            throw new BusinessException("Seule la Manman sol peut demarrer le cycle.");
        }
        if (!"OUVERT".equals(sol.getStatut())) {
            throw new BusinessException("Ce Sol n'est pas en attente de demarrage.");
        }
        sol.setStatut("EN_COURS");
        return solRepository.save(sol);
    }

    /** Les membres d'un Sol dans l'ordre de la rotation. */
    public List<MembreSol> membresDuSol(String solId) {
        return membreSolRepository.findBySolIdOrderByOrdrePassageAsc(solId);
    }

    /** Genere un code court, lisible et unique (ex : K7RT2MQ4). */
    private String genererCodeInvitationUnique() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(LONGUEUR_CODE);
            for (int i = 0; i < LONGUEUR_CODE; i++) {
                sb.append(ALPHABET_CODE.charAt(RANDOM.nextInt(ALPHABET_CODE.length())));
            }
            code = sb.toString();
        } while (solRepository.existsByCodeInvitation(code));
        return code;
    }
}
