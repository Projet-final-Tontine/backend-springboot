package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.TableauDeBordResponse;
import ht.edu.ueh.fds.tontine.dto.TableauDeBordResponse.Echeance;
import ht.edu.ueh.fds.tontine.dto.TableauDeBordResponse.Main;
import ht.edu.ueh.fds.tontine.dto.TableauDeBordResponse.PointEpargne;
import ht.edu.ueh.fds.tontine.entity.Cotisation;
import ht.edu.ueh.fds.tontine.entity.MembreSol;
import ht.edu.ueh.fds.tontine.entity.Tour;
import ht.edu.ueh.fds.tontine.repository.CotisationRepository;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Construit la vue consolidée « Mon activité » (tableau de bord) : indicateurs,
 * projections et courbe d'épargne, calculés à partir des cotisations et des
 * tours de l'utilisateur.
 */
@Service
@RequiredArgsConstructor
public class TableauDeBordService {

    private final MembreSolRepository membreSolRepository;
    private final CotisationRepository cotisationRepository;
    private final TourRepository tourRepository;
    private final PortefeuilleService portefeuilleService;

    @Transactional(readOnly = true)
    public TableauDeBordResponse construire(String utilisateurId) {
        List<MembreSol> membresActifs = membreSolRepository.findByUtilisateurId(utilisateurId)
                .stream().filter(m -> "ACTIF".equals(m.getStatutMembre())).toList();

        BigDecimal totalCotise = BigDecimal.ZERO;
        List<Cotisation> validees = new ArrayList<>();
        Echeance prochaineEcheance = null;

        for (MembreSol m : membresActifs) {
            for (Cotisation c : cotisationRepository.findByMembreSolIdOrderByDateEcheanceDesc(m.getId())) {
                if ("VALIDE".equals(c.getStatut())) {
                    totalCotise = totalCotise.add(c.getMontantPaye());
                    validees.add(c);
                } else if ("EN_ATTENTE".equals(c.getStatut())) {
                    if (prochaineEcheance == null || c.getDateEcheance().isBefore(prochaineEcheance.date())) {
                        prochaineEcheance = new Echeance(
                                c.getDateEcheance(), c.getMontantAttendu(), c.getSol().getNom());
                    }
                }
            }
        }

        // Tours à recevoir (utilisateur bénéficiaire, main pas encore versée).
        List<Tour> aRecevoir = tourRepository.findByBeneficiaireId(utilisateurId).stream()
                .filter(t -> !"CLOTURE".equals(t.getStatut()))
                .sorted(Comparator.comparing(Tour::getDatePrevue))
                .toList();

        BigDecimal totalARecevoir = BigDecimal.ZERO;
        Main prochaineMain = null;
        for (Tour t : aRecevoir) {
            long nbActifs = membreSolRepository.countBySolIdAndStatutMembre(t.getSol().getId(), "ACTIF");
            BigDecimal pot = t.getSol().getMontantCotisation().multiply(BigDecimal.valueOf(nbActifs));
            totalARecevoir = totalARecevoir.add(pot);
            if (prochaineMain == null) {
                prochaineMain = new Main(t.getDatePrevue(), pot, t.getSol().getNom());
            }
        }

        // Courbe d'épargne : cumul des cotisations validées dans le temps.
        validees.sort(Comparator.comparing(this::dateDe));
        List<PointEpargne> epargne = new ArrayList<>();
        BigDecimal cumul = BigDecimal.ZERO;
        for (Cotisation c : validees) {
            cumul = cumul.add(c.getMontantPaye());
            epargne.add(new PointEpargne(dateDe(c), cumul));
        }

        return new TableauDeBordResponse(
                portefeuilleService.solde(utilisateurId),
                totalCotise, totalARecevoir, membresActifs.size(),
                prochaineEcheance, prochaineMain, epargne);
    }

    /** Date retenue pour la courbe : paiement effectif si connu, sinon échéance. */
    private LocalDate dateDe(Cotisation c) {
        return c.getDatePaiementEffectif() != null
                ? c.getDatePaiementEffectif().toLocalDate()
                : c.getDateEcheance();
    }
}
