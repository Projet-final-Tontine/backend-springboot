package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.DisponibiliteResponse;
import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.RechercheUtilisateurResponse;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import ht.edu.ueh.fds.tontine.repository.VerificationKycRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Annuaire public : gestion du username unique (à la Wise/Revolut) et recherche
 * d'un bénéficiaire par username ou e-mail.
 *
 * <p>Le username est l'identifiant public ; l'e-mail et le téléphone restent
 * privés. L'unicité est vérifiée sans tenir compte de la casse
 * ({@code @DudleyJP} == {@code @dudleyjp}).</p>
 */
@Service
@RequiredArgsConstructor
public class AnnuaireService {

    /** Commence par une lettre, puis 3 à 19 caractères parmi lettres/chiffres/_/. (4 à 20 au total). */
    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z][A-Za-z0-9_.]{3,19}$");

    private final UtilisateurRepository utilisateurRepository;
    private final VerificationKycRepository kycRepository;
    private final ht.edu.ueh.fds.tontine.repository.MembreSolRepository membreSolRepository;
    private final ht.edu.ueh.fds.tontine.repository.CotisationRepository cotisationRepository;

    /** Valide le format du username ; lève une exception explicite sinon. */
    public static void validerFormat(String username) {
        if (username == null || !FORMAT.matcher(username).matches()) {
            throw new BusinessException(
                    "Username invalide : 4 à 20 caractères, commence par une lettre, "
                            + "et uniquement lettres, chiffres, « _ » ou « . ».");
        }
    }

    /** Vérifie la disponibilité (format + unicité insensible à la casse). */
    @Transactional(readOnly = true)
    public DisponibiliteResponse verifierDisponibilite(String username) {
        if (username == null || !FORMAT.matcher(username).matches()) {
            return new DisponibiliteResponse(false,
                    "4 à 20 caractères, commence par une lettre (lettres, chiffres, _ ou .).");
        }
        if (utilisateurRepository.existsByUsernameIgnoreCase(username)) {
            return new DisponibiliteResponse(false, "Ce username est déjà utilisé.");
        }
        return new DisponibiliteResponse(true, "Username disponible");
    }

    /**
     * Modifie le username de l'utilisateur : revérifie le format et l'unicité
     * (en ignorant le compte lui-même, pour permettre de re-soumettre son propre
     * username inchangé).
     */
    @Transactional
    public Utilisateur definirUsername(String utilisateurId, String username) {
        validerFormat(username);
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));
        utilisateurRepository.findByUsernameIgnoreCase(username).ifPresent(autre -> {
            if (!autre.getId().equals(utilisateurId)) {
                throw new BusinessException("Ce username est déjà utilisé.");
            }
        });
        u.setUsername(username);
        return utilisateurRepository.save(u);
    }

    /**
     * Recherche un bénéficiaire par username ou e-mail (détection automatique).
     * Exclut le demandeur lui-même et les comptes suspendus/inactifs.
     */
    @Transactional(readOnly = true)
    public RechercheUtilisateurResponse rechercher(String requete, String demandeurId) {
        if (requete == null || requete.isBlank()) {
            throw new BusinessException("Veuillez saisir un username ou un e-mail.");
        }
        String q = requete.trim();
        if (q.startsWith("@")) {
            q = q.substring(1);
        }

        Optional<Utilisateur> trouve = q.contains("@")
                ? utilisateurRepository.findByEmailIgnoreCase(q)
                : utilisateurRepository.findByUsernameIgnoreCase(q);

        Utilisateur u = trouve.orElseThrow(() ->
                new BusinessException("Aucun utilisateur trouvé pour « " + requete + " »."));

        if (u.getId().equals(demandeurId)) {
            throw new BusinessException("Vous ne pouvez pas vous envoyer de l'argent à vous-même.");
        }
        String statut = u.getStatut() == null ? "" : u.getStatut();
        if ("BLOQUE".equals(statut) || "INACTIF".equals(statut)) {
            throw new BusinessException("Ce compte n'est pas disponible.");
        }

        boolean kycVerifie = kycRepository.findByUtilisateurId(u.getId())
                .map(k -> "APPROUVE".equals(k.getStatut()))
                .orElse(false);

        return new RechercheUtilisateurResponse(
                u.getId(), u.getUsername(),
                u.getPrenom() + " " + u.getNom(),
                u.getPhotoUrl(), kycVerifie, scoreFiabilite(u.getId()));
    }

    /**
     * Score de fiabilité simple (0-100) : part des cotisations validées.
     * Renvoie {@code null} si l'utilisateur n'a encore aucun historique.
     */
    @Transactional(readOnly = true)
    public Integer scoreFiabilite(String utilisateurId) {
        long total = 0;
        long validees = 0;
        for (var membre : membreSolRepository.findByUtilisateurId(utilisateurId)) {
            for (var c : cotisationRepository.findByMembreSolIdOrderByDateEcheanceDesc(membre.getId())) {
                if ("VALIDE".equals(c.getStatut()) || "EN_ATTENTE".equals(c.getStatut())
                        || "REJETE".equals(c.getStatut())) {
                    total++;
                    if ("VALIDE".equals(c.getStatut())) {
                        validees++;
                    }
                }
            }
        }
        if (total == 0) {
            return null;
        }
        return (int) Math.round(validees * 100.0 / total);
    }
}

