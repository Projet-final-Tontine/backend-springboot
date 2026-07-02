package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.entity.MembreSol;
import ht.edu.ueh.fds.tontine.entity.Paiement;
import ht.edu.ueh.fds.tontine.entity.Sol;
import ht.edu.ueh.fds.tontine.entity.Tour;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.CotisationRepository;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.PaiementRepository;
import ht.edu.ueh.fds.tontine.repository.SolRepository;
import ht.edu.ueh.fds.tontine.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Regles metier des tours de table :
 * ouverture de la collecte, cloture et versement de la « main » au beneficiaire.
 */
@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;
    private final SolRepository solRepository;
    private final MembreSolRepository membreSolRepository;
    private final CotisationRepository cotisationRepository;
    private final PaiementRepository paiementRepository;
    private final CotisationService cotisationService;

    /**
     * Cas « Declencher le paiement » (Manman sol) :
     * ouvre le tour suivant du cycle et genere les cotisations attendues.
     * Le beneficiaire est designe par l'ordre de passage preetabli.
     */
    @Transactional
    public Tour ouvrirTour(String mamanSolId, String solId, LocalDate datePrevue) {
        Sol sol = solRepository.findById(solId)
                .orElseThrow(() -> new BusinessException("Sol introuvable : " + solId));
        if (!sol.getMamanSol().getId().equals(mamanSolId)) {
            throw new BusinessException("Seule la Manman sol peut declencher un tour.");
        }
        if (!"EN_COURS".equals(sol.getStatut())) {
            throw new BusinessException("Le cycle de ce Sol n'est pas en cours.");
        }
        tourRepository.findBySolIdAndStatut(solId, "OUVERT").ifPresent(t -> {
            throw new BusinessException("Un tour est deja ouvert (n° " + t.getNumeroTour() + ").");
        });

        List<Tour> tours = tourRepository.findBySolIdOrderByNumeroTourAsc(solId);
        int prochainNumero = tours.size() + 1;

        List<MembreSol> rotation = membreSolRepository.findBySolIdOrderByOrdrePassageAsc(solId)
                .stream().filter(m -> "ACTIF".equals(m.getStatutMembre())).toList();
        if (prochainNumero > rotation.size()) {
            throw new BusinessException("Tous les tours de ce cycle ont deja ete joues.");
        }
        MembreSol beneficiaire = rotation.get(prochainNumero - 1);

        Tour tour = tourRepository.save(Tour.builder()
                .sol(sol)
                .beneficiaire(beneficiaire.getUtilisateur())
                .numeroTour(prochainNumero)
                .datePrevue(datePrevue)
                .statut("OUVERT")
                .build());

        cotisationService.genererCotisationsPourTour(tour);
        return tour;
    }

    /**
     * Cas « Payer la main a un tour » (Manman sol) :
     * cloture le tour, calcule la cagnotte validee et ordonne le transfert
     * (Mon Cash) au beneficiaire. Termine le Sol si c'etait le dernier tour.
     */
    @Transactional
    public Tour cloturerEtPayerMain(String mamanSolId, String tourId, String referenceMonCash) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new BusinessException("Tour introuvable : " + tourId));
        Sol sol = tour.getSol();

        if (!sol.getMamanSol().getId().equals(mamanSolId)) {
            throw new BusinessException("Seule la Manman sol peut payer la main.");
        }
        if (!"OUVERT".equals(tour.getStatut())) {
            throw new BusinessException("Ce tour n'est pas ouvert.");
        }

        long enAttente = cotisationRepository.countByTourIdAndStatut(tourId, "EN_ATTENTE");
        if (enAttente > 0) {
            throw new BusinessException(enAttente
                    + " cotisation(s) encore en attente : validez-les ou activez la contrepartie avant de payer la main.");
        }

        BigDecimal main = cotisationRepository.totalValidePourTour(tourId);
        if (main.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Aucune cotisation validee : la main est vide.");
        }

        // Trace du versement de la main au beneficiaire (transfert Mon Cash).
        paiementRepository.save(Paiement.builder()
                .cotisation(cotisationRepository.findByTourId(tourId).get(0))
                .utilisateur(tour.getBeneficiaire())
                .typePaiement("VERSEMENT_MAIN")
                .referenceTransaction(referenceMonCash)
                .montantPaye(main)
                .statutPaiement("SUCCES")
                .dateValidation(LocalDateTime.now())
                .validePar(sol.getMamanSol())
                .build());

        tour.setMontantPotDistribue(main);
        tour.setDateEffectiveDistribution(LocalDateTime.now());
        tour.setStatut("CLOTURE");
        tourRepository.save(tour);

        // Dernier tour du cycle -> le Sol est termine.
        long membresActifs = membreSolRepository.findBySolIdOrderByOrdrePassageAsc(sol.getId())
                .stream().filter(m -> "ACTIF".equals(m.getStatutMembre())).count();
        long toursClotures = tourRepository.findBySolIdOrderByNumeroTourAsc(sol.getId())
                .stream().filter(t -> "CLOTURE".equals(t.getStatut())).count();
        if (toursClotures >= membresActifs) {
            sol.setStatut("TERMINE");
            solRepository.save(sol);
        }

        return tour;
    }

    /** Calendrier des tours d'un Sol (cas « Consulter son compte »). */
    public List<Tour> calendrierDuSol(String solId) {
        return tourRepository.findBySolIdOrderByNumeroTourAsc(solId);
    }
}
