package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.entity.Cotisation;
import ht.edu.ueh.fds.tontine.entity.MembreSol;
import ht.edu.ueh.fds.tontine.entity.Paiement;
import ht.edu.ueh.fds.tontine.entity.Tour;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.CotisationRepository;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.PaiementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regles metier des cotisations :
 * paiement par le membre (via Mon Cash), puis validation par la Manman sol.
 */
@Service
@RequiredArgsConstructor
public class CotisationService {

    private final CotisationRepository cotisationRepository;
    private final PaiementRepository paiementRepository;
    private final MembreSolRepository membreSolRepository;

    /**
     * Cas « Payer le Sol (Cotisation) » :
     * le membre declare son depot ; le paiement part en attente de validation.
     * La reference Mon Cash sert de justificatif.
     */
    @Transactional
    public Paiement payerCotisation(String cotisationId, String utilisateurId, String referenceMonCash) {
        Cotisation cotisation = cotisationRepository.findById(cotisationId)
                .orElseThrow(() -> new BusinessException("Cotisation introuvable : " + cotisationId));

        MembreSol membre = cotisation.getMembreSol();
        if (!membre.getUtilisateur().getId().equals(utilisateurId)) {
            throw new BusinessException("Cette cotisation ne vous appartient pas.");
        }
        if ("VALIDE".equals(cotisation.getStatut())) {
            throw new BusinessException("Cette cotisation est deja reglee et validee.");
        }
        if (referenceMonCash == null || referenceMonCash.isBlank()) {
            throw new BusinessException("La reference de transaction Mon Cash est obligatoire.");
        }
        paiementRepository.findByReferenceTransaction(referenceMonCash).ifPresent(p -> {
            throw new BusinessException("Cette reference Mon Cash a deja ete utilisee.");
        });

        return paiementRepository.save(Paiement.builder()
                .cotisation(cotisation)
                .utilisateur(membre.getUtilisateur())
                .typePaiement("COTISATION")
                .referenceTransaction(referenceMonCash)
                .montantPaye(cotisation.getMontantAttendu())
                .build());
    }

    /**
     * Cas « Valider le paiement » (Manman sol) :
     * approbation humaine de la preuve de depot soumise par le cotisant.
     */
    @Transactional
    public Cotisation validerPaiement(String paiementId, String mamanSolId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new BusinessException("Paiement introuvable : " + paiementId));
        Cotisation cotisation = paiement.getCotisation();
        Utilisateur mamanSol = cotisation.getSol().getMamanSol();

        if (!mamanSol.getId().equals(mamanSolId)) {
            throw new BusinessException("Seule la Manman sol de ce cercle peut valider un paiement.");
        }
        if (!"EN_ATTENTE".equals(paiement.getStatutPaiement())) {
            throw new BusinessException("Ce paiement a deja ete traite.");
        }

        paiement.setStatutPaiement("SUCCES");
        paiement.setValidePar(mamanSol);
        paiement.setDateValidation(LocalDateTime.now());
        paiementRepository.save(paiement);

        cotisation.setMontantPaye(paiement.getMontantPaye());
        cotisation.setStatut("VALIDE");
        cotisation.setDatePaiementEffectif(LocalDateTime.now());
        return cotisationRepository.save(cotisation);
    }

    /** Rejet d'une preuve de depot non conforme (Manman sol). */
    @Transactional
    public Cotisation rejeterPaiement(String paiementId, String mamanSolId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new BusinessException("Paiement introuvable : " + paiementId));
        Cotisation cotisation = paiement.getCotisation();

        if (!cotisation.getSol().getMamanSol().getId().equals(mamanSolId)) {
            throw new BusinessException("Seule la Manman sol de ce cercle peut rejeter un paiement.");
        }
        if (!"EN_ATTENTE".equals(paiement.getStatutPaiement())) {
            throw new BusinessException("Ce paiement a deja ete traite.");
        }

        paiement.setStatutPaiement("ECHEC");
        paiementRepository.save(paiement);

        cotisation.setStatut("REJETE");
        return cotisationRepository.save(cotisation);
    }

    /**
     * Genere les cotisations attendues de tous les membres actifs
     * a l'ouverture d'un tour (appele par TourService).
     */
    @Transactional
    public List<Cotisation> genererCotisationsPourTour(Tour tour) {
        List<MembreSol> membres =
                membreSolRepository.findBySolIdOrderByOrdrePassageAsc(tour.getSol().getId());
        List<Cotisation> cotisations = membres.stream()
                .filter(m -> "ACTIF".equals(m.getStatutMembre()))
                .map(m -> Cotisation.builder()
                        .membreSol(m)
                        .sol(tour.getSol())
                        .tour(tour)
                        .montantAttendu(tour.getSol().getMontantCotisation())
                        .dateEcheance(tour.getDatePrevue())
                        .build())
                .toList();
        return cotisationRepository.saveAll(cotisations);
    }

    /** Historique des cotisations d'un membre (cas « Consulter son compte »). */
    public List<Cotisation> historiqueMembre(String membreSolId) {
        return cotisationRepository.findByMembreSolIdOrderByDateEcheanceDesc(membreSolId);
    }

    /** Toutes les cotisations de l'utilisateur connecte, tous Sols confondus. */
    public List<Cotisation> historiqueUtilisateur(String utilisateurId) {
        return membreSolRepository.findByUtilisateurId(utilisateurId).stream()
                .flatMap(m -> cotisationRepository
                        .findByMembreSolIdOrderByDateEcheanceDesc(m.getId()).stream())
                .toList();
    }

    /** Paiements en attente de validation pour un Sol (Manman sol). */
    public List<Paiement> paiementsEnAttenteDuSol(String solId) {
        return paiementRepository.findByStatutPaiement("EN_ATTENTE").stream()
                .filter(p -> p.getCotisation().getSol().getId().equals(solId))
                .toList();
    }
}
