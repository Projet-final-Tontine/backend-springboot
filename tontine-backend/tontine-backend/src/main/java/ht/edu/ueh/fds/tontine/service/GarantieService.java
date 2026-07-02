package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.entity.ActivationGarantie;
import ht.edu.ueh.fds.tontine.entity.CaisseGarantie;
import ht.edu.ueh.fds.tontine.entity.Cotisation;
import ht.edu.ueh.fds.tontine.entity.MembreSol;
import ht.edu.ueh.fds.tontine.entity.Tour;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.ActivationGarantieRepository;
import ht.edu.ueh.fds.tontine.repository.CaisseGarantieRepository;
import ht.edu.ueh.fds.tontine.repository.CotisationRepository;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.TourRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cas « Activer la contrepartie » (Administrateur) :
 * la caisse de garantie couvre la cotisation d'un membre defaillant pour que
 * le beneficiaire du tour touche sa main a l'heure ; le membre est marque
 * DEFAILLANT et sa dette envers la caisse est tracee.
 */
@Service
@RequiredArgsConstructor
public class GarantieService {

    private final CaisseGarantieRepository caisseGarantieRepository;
    private final ActivationGarantieRepository activationGarantieRepository;
    private final CotisationRepository cotisationRepository;
    private final MembreSolRepository membreSolRepository;
    private final TourRepository tourRepository;
    private final UtilisateurRepository utilisateurRepository;

    /** Alimente la caisse de garantie d'un Sol. */
    @Transactional
    public CaisseGarantie alimenterCaisse(String adminId, String solId, BigDecimal montant) {
        exigerAdmin(adminId);
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant a verser doit etre positif.");
        }
        CaisseGarantie caisse = caisseGarantieRepository.findBySolId(solId)
                .orElseThrow(() -> new BusinessException("Caisse de garantie introuvable pour ce Sol."));
        caisse.setSolde(caisse.getSolde().add(montant));
        return caisseGarantieRepository.save(caisse);
    }

    /**
     * Couvre la cotisation impayee d'un membre : debite la caisse,
     * valide la cotisation au nom de la garantie et marque le membre DEFAILLANT.
     */
    @Transactional
    public ActivationGarantie activerContrepartie(String adminId, String cotisationId, String motif) {
        Utilisateur admin = exigerAdmin(adminId);

        Cotisation cotisation = cotisationRepository.findById(cotisationId)
                .orElseThrow(() -> new BusinessException("Cotisation introuvable : " + cotisationId));
        if ("VALIDE".equals(cotisation.getStatut())) {
            throw new BusinessException("Cette cotisation est deja reglee : rien a couvrir.");
        }
        Tour tour = cotisation.getTour();
        if (tour == null || !"OUVERT".equals(tour.getStatut())) {
            throw new BusinessException("La contrepartie ne s'applique qu'a un tour ouvert.");
        }

        CaisseGarantie caisse = caisseGarantieRepository.findBySolId(cotisation.getSol().getId())
                .orElseThrow(() -> new BusinessException("Caisse de garantie introuvable pour ce Sol."));
        BigDecimal montant = cotisation.getMontantAttendu();
        if (caisse.getSolde().compareTo(montant) < 0) {
            throw new BusinessException("Solde de la caisse insuffisant ("
                    + caisse.getSolde() + " disponible, " + montant + " requis).");
        }

        // 1. Debiter la caisse.
        caisse.setSolde(caisse.getSolde().subtract(montant));
        caisseGarantieRepository.save(caisse);

        // 2. La cotisation est couverte par la garantie.
        cotisation.setMontantPaye(montant);
        cotisation.setStatut("VALIDE");
        cotisation.setDatePaiementEffectif(LocalDateTime.now());
        cotisationRepository.save(cotisation);

        // 3. Marquer le membre defaillant.
        MembreSol membre = cotisation.getMembreSol();
        membre.setStatutMembre("DEFAILLANT");
        membreSolRepository.save(membre);

        // 4. Tracer le secours (dette du membre envers la caisse).
        return activationGarantieRepository.save(ActivationGarantie.builder()
                .caisseGarantie(caisse)
                .tour(tour)
                .membreDefaillant(membre)
                .admin(admin)
                .montant(montant)
                .motif(motif)
                .build());
    }

    /** Le membre defaillant rembourse la caisse : dette soldee, statut retabli. */
    @Transactional
    public ActivationGarantie rembourserGarantie(String adminId, String activationId) {
        exigerAdmin(adminId);
        ActivationGarantie activation = activationGarantieRepository.findById(activationId)
                .orElseThrow(() -> new BusinessException("Activation introuvable : " + activationId));
        if (Boolean.TRUE.equals(activation.getRembourse())) {
            throw new BusinessException("Cette dette est deja remboursee.");
        }

        CaisseGarantie caisse = activation.getCaisseGarantie();
        caisse.setSolde(caisse.getSolde().add(activation.getMontant()));
        caisseGarantieRepository.save(caisse);

        MembreSol membre = activation.getMembreDefaillant();
        membre.setStatutMembre("ACTIF");
        membreSolRepository.save(membre);

        activation.setRembourse(true);
        return activationGarantieRepository.save(activation);
    }

    private Utilisateur exigerAdmin(String adminId) {
        Utilisateur admin = utilisateurRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable : " + adminId));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new BusinessException("Cette action est reservee a l'administrateur.");
        }
        return admin;
    }
}
