package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.PaiementRequests.InitierPaiementResponse;
import ht.edu.ueh.fds.tontine.dto.PaiementRequests.StatutPaiementResponse;
import ht.edu.ueh.fds.tontine.entity.PaiementSimule;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.PaiementSimuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Passerelle de paiement de l'application : reproduit le parcours d'un vrai
 * paiement en ligne (Mon Cash, NatCash, carte Visa/Mastercard, virement).
 *
 * <ol>
 *   <li>{@link #initier} crée un ordre EN_ATTENTE et renvoie une URL de
 *       paiement ;</li>
 *   <li>l'utilisateur finalise sur la page de la passerelle (ouverte dans le
 *       navigateur) ;</li>
 *   <li>{@link #confirmer} crédite (dépôt) ou débite (retrait) le portefeuille
 *       et scelle un bloc au Registre Inviolable.</li>
 * </ol>
 *
 * <p>La confirmation est simulée (aucun compte marchand requis) : idéale pour
 * une démonstration. Un vrai branchement (API Digicel Mon Cash…) ne modifierait
 * que l'étape de confirmation.</p>
 */
@Service
@RequiredArgsConstructor
public class PasserelleService {

    private static final Set<String> MOYENS =
            Set.of("MONCASH", "NATCASH", "VISA", "MASTERCARD", "VIREMENT");
    private static final BigDecimal MONTANT_MAX = new BigDecimal("1000000");

    private final SecureRandom aleatoire = new SecureRandom();

    private final PaiementSimuleRepository paiementRepository;
    private final PortefeuilleService portefeuilleService;
    private final KycService kycService;

    @Value("${app.public-url:http://localhost:8080}")
    private String urlPublique;

    /** Crée un ordre de paiement et renvoie l'URL de la page à ouvrir. */
    @Transactional
    public InitierPaiementResponse initier(String utilisateurId, String sens,
                                           String moyen, BigDecimal montant) {
        // Règle stricte : aucun dépôt ni retrait tant que l'identité n'est pas vérifiée.
        kycService.exigerVerifie(utilisateurId);

        String s = sens == null ? "" : sens.trim().toUpperCase();
        String m = moyen == null ? "" : moyen.trim().toUpperCase();
        if (!s.equals("DEPOT") && !s.equals("RETRAIT")) {
            throw new BusinessException("Sens de paiement invalide.");
        }
        if (!MOYENS.contains(m)) {
            throw new BusinessException("Moyen de paiement non pris en charge.");
        }
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant doit etre positif.");
        }
        if (montant.compareTo(MONTANT_MAX) > 0) {
            throw new BusinessException("Montant trop eleve.");
        }
        // Pour un retrait, on verifie le solde des l'initialisation (retour clair).
        if (s.equals("RETRAIT")
                && portefeuilleService.solde(utilisateurId).compareTo(montant) < 0) {
            throw new BusinessException("Solde insuffisant pour ce retrait.");
        }

        PaiementSimule ordre = PaiementSimule.builder()
                .id(UUID.randomUUID().toString())
                .utilisateurId(utilisateurId)
                .sens(s)
                .moyen(m)
                .montant(montant)
                .statut("EN_ATTENTE")
                .reference(genererReference())
                .dateCreation(LocalDateTime.now())
                .build();
        paiementRepository.save(ordre);

        String redirectUrl = urlPublique.replaceAll("/+$", "") + "/pay/" + ordre.getId();
        return new InitierPaiementResponse(ordre.getId(), ordre.getReference(),
                redirectUrl, ordre.getStatut());
    }

    /**
     * Confirme le paiement : credite/debite le portefeuille et scelle le
     * registre. Idempotent : une seconde confirmation ne rejoue pas l'operation.
     */
    @Transactional
    public PaiementSimule confirmer(String orderId) {
        PaiementSimule ordre = paiementRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Ordre de paiement introuvable."));
        if ("PAYE".equals(ordre.getStatut())) {
            return ordre; // deja traite
        }
        if (!"EN_ATTENTE".equals(ordre.getStatut())) {
            throw new BusinessException("Cet ordre de paiement n'est plus valide.");
        }

        if ("DEPOT".equals(ordre.getSens())) {
            portefeuilleService.deposerParMoyen(ordre.getUtilisateurId(),
                    ordre.getMontant(), ordre.getMoyen(), ordre.getReference());
        } else {
            portefeuilleService.retirerParMoyen(ordre.getUtilisateurId(),
                    ordre.getMontant(), ordre.getMoyen(), ordre.getReference());
        }

        ordre.setStatut("PAYE");
        ordre.setDateConfirmation(LocalDateTime.now());
        return paiementRepository.save(ordre);
    }

    /** Ordre de paiement (pour construire la page de la passerelle). */
    @Transactional(readOnly = true)
    public PaiementSimule get(String orderId) {
        return paiementRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Ordre de paiement introuvable."));
    }

    /** État de l'ordre, interrogé par l'application au retour du navigateur. */
    @Transactional(readOnly = true)
    public StatutPaiementResponse statut(String orderId) {
        PaiementSimule o = get(orderId);
        return new StatutPaiementResponse(o.getId(), o.getSens(), o.getMoyen(),
                o.getMontant(), o.getStatut(), o.getReference());
    }

    private String genererReference() {
        StringBuilder sb = new StringBuilder("SOL-PAY-");
        for (int i = 0; i < 8; i++) {
            sb.append("0123456789ABCDEF".charAt(aleatoire.nextInt(16)));
        }
        return sb.toString();
    }
}
