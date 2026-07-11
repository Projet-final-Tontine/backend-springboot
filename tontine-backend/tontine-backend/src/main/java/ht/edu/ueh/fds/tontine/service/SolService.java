package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.entity.CaisseGarantie;
import ht.edu.ueh.fds.tontine.entity.MembreSol;
import ht.edu.ueh.fds.tontine.entity.Sol;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.dto.SolResponse;
import ht.edu.ueh.fds.tontine.dto.MembreSolResponse;
import ht.edu.ueh.fds.tontine.dto.SolDetailResponse;
import ht.edu.ueh.fds.tontine.entity.Tour;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.CaisseGarantieRepository;
import ht.edu.ueh.fds.tontine.repository.CotisationRepository;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.SolRepository;
import ht.edu.ueh.fds.tontine.repository.TourRepository;
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
    private final TourRepository tourRepository;
    private final CotisationRepository cotisationRepository;

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

    /**
     * Demarre officiellement le cycle : plus aucune adhesion possible.
     * La reponse est construite ici, session ouverte, pour initialiser la
     * relation paresseuse vers la Manman sol (sinon « no session »).
     */
    @Transactional
    public SolResponse demarrerCycle(String mamanSolId, String solId) {
        Sol sol = solRepository.findById(solId)
                .orElseThrow(() -> new BusinessException("Sol introuvable : " + solId));
        if (!sol.getMamanSol().getId().equals(mamanSolId)) {
            throw new BusinessException("Seule la Manman sol peut demarrer le cycle.");
        }
        if (!"OUVERT".equals(sol.getStatut())) {
            throw new BusinessException("Ce Sol n'est pas en attente de demarrage.");
        }
        sol.setStatut("EN_COURS");
        return SolResponse.from(solRepository.save(sol));
    }

    /**
     * Les membres d'un Sol dans l'ordre de la rotation.
     * Conversion en DTO dans la transaction pour initialiser les relations
     * paresseuses (utilisateur, sol) avant la fermeture de la session.
     */
    @Transactional(readOnly = true)
    public List<MembreSolResponse> membresDuSol(String solId) {
        return membreSolRepository.findBySolIdOrderByOrdrePassageAsc(solId).stream()
                .map(MembreSolResponse::from)
                .toList();
    }

    /**
     * Liste des Sols auxquels participe l'utilisateur connecte.
     * La conversion en {@link SolResponse} se fait ici, pendant que la session
     * Hibernate est encore ouverte, afin d'initialiser les relations paresseuses
     * (ex. la Manman sol). Sinon le controleur declenche « Could not initialize
     * proxy - no session ».
     */
    @Transactional(readOnly = true)
    public List<SolResponse> solsDeLUtilisateur(String utilisateurId) {
        return membreSolRepository.findByUtilisateurId(utilisateurId).stream()
                .map(MembreSol::getSol)
                .map(SolResponse::from)
                .toList();
    }

    /**
     * Vue complete d'un Sol pour l'ecran de detail :
     * progression du cycle, tour en cours, prochain beneficiaire,
     * etat des cotisations de chaque membre et position de l'utilisateur.
     * Toute la reponse est construite ici, session ouverte (relations paresseuses).
     */
    @Transactional(readOnly = true)
    public SolDetailResponse detailDuSol(String utilisateurId, String solId) {
        Sol sol = solRepository.findById(solId)
                .orElseThrow(() -> new BusinessException("Sol introuvable : " + solId));

        List<MembreSol> membresActifs = membreSolRepository
                .findBySolIdOrderByOrdrePassageAsc(solId).stream()
                .filter(m -> "ACTIF".equals(m.getStatutMembre()))
                .toList();

        List<Tour> tours = tourRepository.findBySolIdOrderByNumeroTourAsc(solId);
        List<SolDetailResponse.TourInfo> toursInfo = tours.stream()
                .map(t -> new SolDetailResponse.TourInfo(
                        t.getId(), t.getNumeroTour(),
                        t.getBeneficiaire().getId(),
                        t.getBeneficiaire().getPrenom() + " " + t.getBeneficiaire().getNom(),
                        t.getDatePrevue(), t.getStatut(), t.getMontantPotDistribue()))
                .toList();

        int toursJoues = (int) tours.stream()
                .filter(t -> "CLOTURE".equals(t.getStatut())).count();

        // Tour en cours de collecte (au plus un par Sol).
        SolDetailResponse.TourInfo tourCourant = toursInfo.stream()
                .filter(t -> "OUVERT".equals(t.statut()))
                .findFirst().orElse(null);

        // Qui a paye / qui doit encore payer pour le tour en cours.
        List<SolDetailResponse.EtatCotisation> etats = tourCourant == null
                ? List.of()
                : cotisationRepository.findByTourId(tourCourant.id()).stream()
                        .map(c -> new SolDetailResponse.EtatCotisation(
                                c.getMembreSol().getUtilisateur().getId(),
                                c.getMembreSol().getUtilisateur().getPrenom() + " "
                                        + c.getMembreSol().getUtilisateur().getNom(),
                                c.getMembreSol().getUtilisateur().getPhotoUrl(),
                                c.getMembreSol().getOrdrePassage(),
                                c.getMontantAttendu(), c.getStatut(), c.getDateEcheance()))
                        .sorted(java.util.Comparator.comparing(
                                e -> e.ordre() == null ? Integer.MAX_VALUE : e.ordre()))
                        .toList();

        // La rotation complete, avec avatars (affichage de la liste des membres).
        List<SolDetailResponse.MembreInfo> membresInfo = membresActifs.stream()
                .map(m -> new SolDetailResponse.MembreInfo(
                        m.getUtilisateur().getId(),
                        m.getUtilisateur().getPrenom() + " " + m.getUtilisateur().getNom(),
                        m.getUtilisateur().getPhotoUrl(),
                        m.getOrdrePassage(),
                        m.getStatutMembre()))
                .toList();

        // Position de l'utilisateur connecte dans la rotation.
        SolDetailResponse.PositionMembre maPosition = membresActifs.stream()
                .filter(m -> m.getUtilisateur().getId().equals(utilisateurId))
                .findFirst()
                .map(m -> {
                    int ordre = m.getOrdrePassage() == null ? 0 : m.getOrdrePassage();
                    // Date reelle si le tour existe deja, sinon estimation
                    // a partir de la date de debut et de la frequence.
                    var tourDuMembre = tours.stream()
                            .filter(t -> t.getNumeroTour() != null && t.getNumeroTour() == ordre)
                            .findFirst();
                    java.time.LocalDate date;
                    boolean estimee;
                    if (tourDuMembre.isPresent()) {
                        date = tourDuMembre.get().getDatePrevue();
                        estimee = false;
                    } else if (sol.getDateDebut() != null && ordre > 0) {
                        date = "HEBDOMADAIRE".equalsIgnoreCase(sol.getFrequence())
                                ? sol.getDateDebut().plusWeeks(ordre - 1L)
                                : sol.getDateDebut().plusMonths(ordre - 1L);
                        estimee = true;
                    } else {
                        date = null;
                        estimee = true;
                    }
                    return new SolDetailResponse.PositionMembre(
                            ordre, membresActifs.size(), date, estimee);
                })
                .orElse(null);

        // ----- Sante du Sol : ponctualite des cotisations arrivees a echeance -----
        var toutesCotisations = cotisationRepository.findBySolId(solId);
        var aujourdHui = java.time.LocalDate.now();
        int evaluees = 0;
        int reglesATemps = 0;
        for (var c : toutesCotisations) {
            boolean payee = "VALIDE".equals(c.getStatut());
            boolean echue = c.getDateEcheance() != null && c.getDateEcheance().isBefore(aujourdHui);
            if (payee || echue) {
                evaluees++;
                if (payee && (c.getDatePaiementEffectif() == null
                        || !c.getDatePaiementEffectif().toLocalDate().isAfter(c.getDateEcheance()))) {
                    reglesATemps++;
                }
            }
        }
        int scoreSante = evaluees == 0 ? 100 : (reglesATemps * 100) / evaluees;
        String niveauSante = scoreSante >= 80 ? "EXCELLENT" : scoreSante >= 50 ? "MOYEN" : "RISQUE";
        var sante = new SolDetailResponse.SanteSol(scoreSante, niveauSante);

        // ----- Journal automatique : deduit des donnees, sans table dediee -----
        var journal = new java.util.ArrayList<SolDetailResponse.EvenementJournal>();
        for (var m : membresActifs) {
            if (m.getDateAdhesion() != null) {
                journal.add(new SolDetailResponse.EvenementJournal(
                        m.getDateAdhesion(), "ADHESION",
                        m.getUtilisateur().getPrenom() + " " + m.getUtilisateur().getNom(), null));
            }
        }
        for (var c : toutesCotisations) {
            String nom = c.getMembreSol().getUtilisateur().getPrenom() + " "
                    + c.getMembreSol().getUtilisateur().getNom();
            if ("VALIDE".equals(c.getStatut()) && c.getDatePaiementEffectif() != null) {
                journal.add(new SolDetailResponse.EvenementJournal(
                        c.getDatePaiementEffectif(), "PAIEMENT", nom, c.getMontantPaye()));
            } else if (!"VALIDE".equals(c.getStatut())
                    && c.getDateEcheance() != null && c.getDateEcheance().isBefore(aujourdHui)) {
                journal.add(new SolDetailResponse.EvenementJournal(
                        c.getDateEcheance().atStartOfDay(), "RETARD", nom, c.getMontantAttendu()));
            }
        }
        for (var t : tours) {
            String beneficiaire = t.getBeneficiaire().getPrenom() + " " + t.getBeneficiaire().getNom();
            if (t.getDateCreation() != null) {
                journal.add(new SolDetailResponse.EvenementJournal(
                        t.getDateCreation(), "TOUR_OUVERT", "n°" + t.getNumeroTour(), null));
            }
            if ("CLOTURE".equals(t.getStatut()) && t.getDateEffectiveDistribution() != null) {
                journal.add(new SolDetailResponse.EvenementJournal(
                        t.getDateEffectiveDistribution(), "MAIN", beneficiaire, t.getMontantPotDistribue()));
            }
        }
        journal.sort(java.util.Comparator.comparing(
                SolDetailResponse.EvenementJournal::date).reversed());

        return new SolDetailResponse(
                SolResponse.from(sol),
                membresActifs.size(),
                toursJoues,
                membresActifs.size(),
                tourCourant,
                toursInfo,
                etats,
                membresInfo,
                maPosition,
                sante,
                journal);
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
