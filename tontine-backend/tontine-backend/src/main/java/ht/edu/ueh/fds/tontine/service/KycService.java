package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.KycRequests.KycEtatResponse;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.entity.VerificationKyc;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import ht.edu.ueh.fds.tontine.repository.VerificationKycRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Vérification d'identité (KYC). L'utilisateur confirme d'abord son identité,
 * puis soumet une pièce (carte d'identité, passeport ou permis).
 *
 * <p>En mode démonstration ({@code app.kyc.auto-approve=true}, défaut), la
 * soumission est approuvée automatiquement — parcours autonome pour une
 * présentation. En le passant à {@code false}, la validation revient à un
 * administrateur.</p>
 */
@Service
public class KycService {

    private static final Set<String> TYPES = Set.of("CARTE_IDENTITE", "PASSEPORT", "PERMIS");

    private final UtilisateurRepository utilisateurRepository;
    private final VerificationKycRepository kycRepository;
    private final boolean autoApprouver;

    public KycService(UtilisateurRepository utilisateurRepository,
                      VerificationKycRepository kycRepository,
                      @Value("${app.kyc.auto-approve:true}") boolean autoApprouver) {
        this.utilisateurRepository = utilisateurRepository;
        this.kycRepository = kycRepository;
        this.autoApprouver = autoApprouver;
    }

    /** État courant : identité pré-remplie + statut de vérification. */
    @Transactional(readOnly = true)
    public KycEtatResponse etat(String utilisateurId) {
        Utilisateur u = exiger(utilisateurId);
        VerificationKyc kyc = kycRepository.findByUtilisateurId(utilisateurId).orElse(null);
        return construire(u, kyc);
    }

    /** Étape 1 : confirme/corrige l'identité (modifiable uniquement ici). */
    @Transactional
    public KycEtatResponse majIdentite(String utilisateurId, String nom, String prenom,
                                       String dateNaissance, String adresse) {
        Utilisateur u = exiger(utilisateurId);
        if (nom != null && !nom.isBlank()) u.setNom(nom.trim());
        if (prenom != null && !prenom.isBlank()) u.setPrenom(prenom.trim());
        if (adresse != null && !adresse.isBlank()) u.setAdresse(adresse.trim());
        if (dateNaissance != null && !dateNaissance.isBlank()) {
            try {
                u.setDateNaissance(LocalDate.parse(dateNaissance.trim()));
            } catch (Exception e) {
                throw new BusinessException("Date de naissance invalide (format attendu : AAAA-MM-JJ).");
            }
        }
        utilisateurRepository.save(u);
        return construire(u, kycRepository.findByUtilisateurId(utilisateurId).orElse(null));
    }

    /** Étape finale : soumission des documents. */
    @Transactional
    public KycEtatResponse soumettre(String utilisateurId, String typeDocument,
                                     String rectoUrl, String versoUrl) {
        String type = typeDocument == null ? "" : typeDocument.trim().toUpperCase();
        if (!TYPES.contains(type)) {
            throw new BusinessException("Type de pièce non pris en charge.");
        }
        if (rectoUrl == null || rectoUrl.isBlank()) {
            throw new BusinessException("La photo de la pièce est requise.");
        }
        // La carte d'identité et le permis exigent le verso.
        if ((type.equals("CARTE_IDENTITE") || type.equals("PERMIS"))
                && (versoUrl == null || versoUrl.isBlank())) {
            throw new BusinessException("Le verso de la pièce est requis.");
        }

        VerificationKyc kyc = kycRepository.findByUtilisateurId(utilisateurId)
                .orElseGet(() -> VerificationKyc.builder()
                        .id(UUID.randomUUID().toString())
                        .utilisateurId(utilisateurId)
                        .build());
        kyc.setTypeDocument(type);
        kyc.setRectoUrl(rectoUrl);
        kyc.setVersoUrl(versoUrl);
        kyc.setDateSoumission(LocalDateTime.now());
        Utilisateur u = exiger(utilisateurId);
        if (autoApprouver) {
            kyc.setStatut("APPROUVE");
            kyc.setDateDecision(LocalDateTime.now());
            // En mode démo, l'approbation active le compte (identité confirmée).
            if (!"BLOQUE".equals(u.getStatut())) {
                u.setStatut("ACTIF");
                utilisateurRepository.save(u);
            }
        } else {
            kyc.setStatut("SOUMIS");
            kyc.setDateDecision(null);
        }
        kycRepository.save(kyc);

        return construire(u, kyc);
    }

    /** Vrai si l'identité de l'utilisateur est vérifiée (dossier KYC approuvé). */
    @Transactional(readOnly = true)
    public boolean estVerifie(String utilisateurId) {
        return kycRepository.findByUtilisateurId(utilisateurId)
                .map(k -> "APPROUVE".equals(k.getStatut()))
                .orElse(false);
    }

    /**
     * Barrière stricte : refuse toute opération financière (dépôt, retrait,
     * transfert) tant que l'identité n'est pas vérifiée. À appeler au tout début
     * des opérations concernées.
     */
    public void exigerVerifie(String utilisateurId) {
        if (!estVerifie(utilisateurId)) {
            throw new BusinessException(
                    "Vérification d'identité requise : vérifiez votre identité (KYC) "
                    + "avant d'effectuer un dépôt, un retrait ou un transfert.");
        }
    }

    private KycEtatResponse construire(Utilisateur u, VerificationKyc kyc) {
        return new KycEtatResponse(
                u.getNom(), u.getPrenom(),
                u.getDateNaissance() == null ? null : u.getDateNaissance().toString(),
                u.getAdresse(),
                kyc == null ? "NON_SOUMIS" : kyc.getStatut(),
                kyc == null ? null : kyc.getTypeDocument(),
                kyc == null || kyc.getDateSoumission() == null ? null : kyc.getDateSoumission().toString());
    }

    private Utilisateur exiger(String utilisateurId) {
        return utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
    }
}
