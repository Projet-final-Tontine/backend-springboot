package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.entity.Notification;
import ht.edu.ueh.fds.tontine.entity.Tour;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.repository.CotisationRepository;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.NotificationRepository;
import ht.edu.ueh.fds.tontine.repository.TourRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Acteur interne « Le Systeme » (automate temporel) :
 * - purge des comptes inactifs depuis plus d'un an ;
 * - rappels automatiques avant chaque echeance de versement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemeAutomatiqueService {

    private static final int JOURS_INACTIVITE_AVANT_PURGE = 365;
    private static final int JOURS_AVANT_ECHEANCE_POUR_RAPPEL = 2;

    private final UtilisateurRepository utilisateurRepository;
    private final MembreSolRepository membreSolRepository;
    private final TourRepository tourRepository;
    private final CotisationRepository cotisationRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Cas « Supprimer un compte inactif (1 an) » — routine quotidienne (2h00).
     * Le compte est supprime s'il n'appartient a aucun Sol ;
     * sinon il est marque INACTIF (les donnees comptables du groupe restent auditables).
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgerComptesInactifs() {
        LocalDateTime limite = LocalDateTime.now().minusDays(JOURS_INACTIVITE_AVANT_PURGE);
        List<Utilisateur> inactifs = utilisateurRepository.findByDerniereConnexionBefore(limite);

        for (Utilisateur utilisateur : inactifs) {
            boolean membreDUnSol = !membreSolRepository.findByUtilisateurId(utilisateur.getId()).isEmpty();
            if (membreDUnSol) {
                utilisateur.setStatut("INACTIF");
                utilisateurRepository.save(utilisateur);
                log.info("Compte {} marque INACTIF (membre d'un Sol, purge impossible).", utilisateur.getId());
            } else {
                utilisateurRepository.delete(utilisateur);
                log.info("Compte {} purge apres {} jours d'inactivite.", utilisateur.getId(), JOURS_INACTIVITE_AVANT_PURGE);
            }
        }
    }

    /**
     * Cas « Envoi automatique de rappels » — routine quotidienne (8h00) :
     * notifie les membres n'ayant pas encore cotise pour un tour dont
     * l'echeance tombe dans les 2 prochains jours.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void envoyerRappelsEcheance() {
        LocalDate aujourdHui = LocalDate.now();
        List<Tour> toursProches = tourRepository.findByStatutAndDatePrevueBetween(
                "OUVERT", aujourdHui, aujourdHui.plusDays(JOURS_AVANT_ECHEANCE_POUR_RAPPEL));

        for (Tour tour : toursProches) {
            cotisationRepository.findByTourId(tour.getId()).stream()
                    .filter(c -> "EN_ATTENTE".equals(c.getStatut()))
                    .forEach(c -> notificationRepository.save(Notification.builder()
                            .utilisateur(c.getMembreSol().getUtilisateur())
                            .type("RAPPEL_ECHEANCE")
                            .sujet("Rappel de cotisation — " + tour.getSol().getNom())
                            .message("Votre cotisation de " + c.getMontantAttendu()
                                    + " HTG pour le tour n° " + tour.getNumeroTour()
                                    + " est attendue au plus tard le " + c.getDateEcheance() + ".")
                            .build()));
        }
        log.info("Rappels d'echeance envoyes pour {} tour(s).", toursProches.size());
    }
}
