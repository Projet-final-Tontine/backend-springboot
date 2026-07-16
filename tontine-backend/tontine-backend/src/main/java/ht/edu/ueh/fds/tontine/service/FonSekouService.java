package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.FonSekouResponse;
import ht.edu.ueh.fds.tontine.entity.*;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fon Sekou : caisse de solidarité d'un Sol. Les membres l'alimentent ; un
 * membre frappé par un malheur fait une demande de secours ; le groupe vote ;
 * la Manman sol clôture. Si le vote est favorable et la caisse suffisante,
 * le montant est versé dans le portefeuille du demandeur.
 */
@Service
@RequiredArgsConstructor
public class FonSekouService {

    private final FonSekouRepository fonSekouRepository;
    private final ContributionSekouRepository contributionRepository;
    private final DemandeSekouRepository demandeRepository;
    private final VoteSekouRepository voteRepository;
    private final MembreSolRepository membreSolRepository;
    private final SolRepository solRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PortefeuilleService portefeuilleService;

    // ---------------------------------------------------------------- lecture

    /** État de la caisse + demandes (réservé aux membres actifs et à la Manman sol). */
    @Transactional
    public FonSekouResponse etat(String utilisateurId, String solId) {
        Sol sol = exigerSol(solId);
        exigerAcces(utilisateurId, sol);
        BigDecimal solde = getOuCreer(solId).getSolde();
        boolean estMamanSol = sol.getMamanSol().getId().equals(utilisateurId);

        List<FonSekouResponse.DemandeInfo> demandes = demandeRepository
                .findBySolIdOrderByDateCreationDesc(solId).stream()
                .map(d -> versInfo(d, utilisateurId))
                .toList();

        return new FonSekouResponse(solde, estMamanSol, demandes);
    }

    // ------------------------------------------------------------- alimenter

    /** Un membre alimente la caisse : son portefeuille est débité, la caisse créditée. */
    @Transactional
    public FonSekouResponse contribuer(String utilisateurId, String solId, BigDecimal montant) {
        Sol sol = exigerSol(solId);
        exigerAcces(utilisateurId, sol);
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant de la contribution doit être positif.");
        }
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));

        // Débit du portefeuille du membre (lève une erreur si solde insuffisant).
        portefeuilleService.debiter(utilisateurId, montant, "FON_SEKOU", "Contribution au Fon Sekou");

        FonSekou caisse = getOuCreer(solId);
        caisse.setSolde(caisse.getSolde().add(montant));
        fonSekouRepository.save(caisse);

        contributionRepository.save(ContributionSekou.builder()
                .solId(solId)
                .contributeur(u)
                .montant(montant)
                .build());

        return etat(utilisateurId, solId);
    }

    // -------------------------------------------------------------- demander

    /** Un membre demande un secours (une seule demande en attente à la fois). */
    @Transactional
    public FonSekouResponse demander(String utilisateurId, String solId, String type,
                                     BigDecimal montant, String motif, String justificatifUrl) {
        Sol sol = exigerSol(solId);
        exigerAcces(utilisateurId, sol);
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant demandé doit être positif.");
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Veuillez décrire le motif de votre demande.");
        }
        if (demandeRepository.existsByDemandeurIdAndSolIdAndStatut(utilisateurId, solId, "EN_ATTENTE")) {
            throw new BusinessException("Vous avez déjà une demande en attente dans ce Sol.");
        }
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));

        demandeRepository.save(DemandeSekou.builder()
                .solId(solId)
                .demandeur(u)
                .type(typeValide(type))
                .montantDemande(montant)
                .motif(motif.trim())
                .justificatifUrl(justificatifUrl == null || justificatifUrl.isBlank() ? null : justificatifUrl)
                .statut("EN_ATTENTE")
                .build());

        return etat(utilisateurId, solId);
    }

    // ----------------------------------------------------------------- voter

    /** Un membre vote pour ou contre une demande (une voix, modifiable). */
    @Transactional
    public FonSekouResponse voter(String utilisateurId, String demandeId, boolean pour) {
        DemandeSekou demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new BusinessException("Demande introuvable."));
        Sol sol = exigerSol(demande.getSolId());
        exigerAcces(utilisateurId, sol);
        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Cette demande est déjà clôturée.");
        }
        if (demande.getDemandeur().getId().equals(utilisateurId)) {
            throw new BusinessException("Vous ne pouvez pas voter sur votre propre demande.");
        }
        VoteSekou vote = voteRepository.findByDemandeIdAndVotantId(demandeId, utilisateurId)
                .orElseGet(() -> VoteSekou.builder()
                        .demandeId(demandeId)
                        .votant(demande.getDemandeur()) // remplacé juste après
                        .build());
        vote.setVotant(utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable.")));
        vote.setPour(pour);
        voteRepository.save(vote);

        return etat(utilisateurId, sol.getId());
    }

    // -------------------------------------------------------------- clôturer

    /**
     * La Manman sol clôture une demande. Si les « pour » l'emportent et que la
     * caisse est suffisante, le montant est versé au demandeur (statut PAYE),
     * sinon la demande est rejetée.
     */
    @Transactional
    public FonSekouResponse cloturer(String mamanSolId, String demandeId) {
        DemandeSekou demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new BusinessException("Demande introuvable."));
        Sol sol = exigerSol(demande.getSolId());
        if (!sol.getMamanSol().getId().equals(mamanSolId)) {
            throw new BusinessException("Seule la Manman sol peut clôturer une demande.");
        }
        if (!"EN_ATTENTE".equals(demande.getStatut())) {
            throw new BusinessException("Cette demande est déjà clôturée.");
        }

        long pour = voteRepository.countByDemandeIdAndPour(demandeId, true);
        long contre = voteRepository.countByDemandeIdAndPour(demandeId, false);
        FonSekou caisse = getOuCreer(sol.getId());

        boolean approuve = pour > contre
                && caisse.getSolde().compareTo(demande.getMontantDemande()) >= 0;

        if (approuve) {
            caisse.setSolde(caisse.getSolde().subtract(demande.getMontantDemande()));
            fonSekouRepository.save(caisse);
            portefeuilleService.crediter(demande.getDemandeur().getId(),
                    demande.getMontantDemande(), "SECOURS_SEKOU",
                    "Secours du Fon Sekou approuvé par le groupe");
            demande.setStatut("PAYE");
        } else {
            demande.setStatut("REJETE");
        }
        demandeRepository.save(demande);

        return etat(mamanSolId, sol.getId());
    }

    // --------------------------------------------------------------- helpers

    private FonSekou getOuCreer(String solId) {
        return fonSekouRepository.findBySolId(solId)
                .orElseGet(() -> fonSekouRepository.save(FonSekou.builder()
                        .solId(solId)
                        .solde(BigDecimal.ZERO)
                        .build()));
    }

    private Sol exigerSol(String solId) {
        return solRepository.findById(solId)
                .orElseThrow(() -> new BusinessException("Sol introuvable : " + solId));
    }

    /** Seuls la Manman sol et les membres ACTIF accèdent au Fon Sekou. */
    private void exigerAcces(String utilisateurId, Sol sol) {
        boolean mamanSol = sol.getMamanSol() != null
                && sol.getMamanSol().getId().equals(utilisateurId);
        boolean actif = membreSolRepository
                .existsByUtilisateurIdAndSolIdAndStatutMembre(utilisateurId, sol.getId(), "ACTIF");
        if (!mamanSol && !actif) {
            throw new BusinessException("Vous n'avez pas accès à ce Sol.");
        }
    }

    private String typeValide(String type) {
        String t = type == null ? "" : type.trim().toUpperCase();
        return switch (t) {
            case "DECES", "MALADIE", "CATASTROPHE", "AUTRE" -> t;
            default -> "AUTRE";
        };
    }

    private FonSekouResponse.DemandeInfo versInfo(DemandeSekou d, String utilisateurId) {
        long nbPour = voteRepository.countByDemandeIdAndPour(d.getId(), true);
        long nbContre = voteRepository.countByDemandeIdAndPour(d.getId(), false);
        Boolean monVote = voteRepository.findByDemandeIdAndVotantId(d.getId(), utilisateurId)
                .map(VoteSekou::isPour).orElse(null);
        return new FonSekouResponse.DemandeInfo(
                d.getId(),
                d.getDemandeur().getId(),
                d.getDemandeur().getPrenom() + " " + d.getDemandeur().getNom(),
                d.getType(),
                d.getMontantDemande(),
                d.getMotif(),
                d.getJustificatifUrl(),
                d.getStatut(),
                nbPour,
                nbContre,
                monVote,
                d.getDemandeur().getId().equals(utilisateurId),
                d.getDateCreation());
    }
}
