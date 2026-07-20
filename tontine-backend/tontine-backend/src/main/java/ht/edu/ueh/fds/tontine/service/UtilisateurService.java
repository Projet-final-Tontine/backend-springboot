package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.entity.JetonReinitialisation;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.JetonReinitialisationRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Regles metier des comptes :
 * inscription, connexion, profil, activation/desactivation (admin),
 * et renouvellement de mot de passe par code SMS.
 *
 * Les mots de passe sont haches en BCrypt (via {@link PasswordEncoder}).
 */
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VALIDITE_JETON_MINUTES = 15;

    private final UtilisateurRepository utilisateurRepository;
    private final JetonReinitialisationRepository jetonRepository;
    private final PasswordEncoder passwordEncoder;

    /** Cas « S'inscrire » : creation du compte (auto-active, l'admin peut le bloquer). */
    @Transactional
    public Utilisateur inscrire(Utilisateur utilisateur, String motDePasseClair) {
        if (utilisateurRepository.existsByTelephone(utilisateur.getTelephone())) {
            throw new BusinessException("Ce numero de telephone est deja utilise.");
        }
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new BusinessException("Cet email est deja utilise.");
        }
        // Username public obligatoire : format valide + unicite insensible a la casse.
        AnnuaireService.validerFormat(utilisateur.getUsername());
        if (utilisateurRepository.existsByUsernameIgnoreCase(utilisateur.getUsername())) {
            throw new BusinessException("Ce username est deja utilise.");
        }
        // CIN/NIF facultatif : s'il est vide, on lui attribue une valeur unique
        // (basee sur le telephone, deja unique) pour ne JAMAIS entrer en collision.
        // Sinon, on verifie l'unicite normalement.
        String cinNif = utilisateur.getCinNif();
        if (cinNif == null || cinNif.isBlank()) {
            utilisateur.setCinNif("SANS-CIN-" + utilisateur.getTelephone());
        } else if (utilisateurRepository.existsByCinNif(cinNif)) {
            throw new BusinessException("Ce CIN/NIF est deja enregistre.");
        }
        if (motDePasseClair == null || motDePasseClair.length() < 8) {
            throw new BusinessException("Le mot de passe doit compter au moins 8 caracteres.");
        }
        utilisateur.setMotDePasseHache(passwordEncoder.encode(motDePasseClair));
        if (utilisateur.getRole() == null) {
            utilisateur.setRole("MEMBRE");
        }
        // Auto-activation a l'inscription : permet de creer/rejoindre un Sol
        // immediatement. L'administrateur garde le pouvoir de bloquer un compte.
        utilisateur.setStatut("ACTIF");
        return utilisateurRepository.save(utilisateur);
    }

    /** Cas « Changer son mot de passe » (utilisateur connecte). */
    @Transactional
    public void changerMotDePasse(String utilisateurId, String ancien, String nouveau) {
        Utilisateur utilisateur = exiger(utilisateurId);
        if (ancien == null || !passwordEncoder.matches(ancien, utilisateur.getMotDePasseHache())) {
            throw new BusinessException("L'ancien mot de passe est incorrect.");
        }
        if (nouveau == null || nouveau.length() < 8) {
            throw new BusinessException("Le mot de passe doit compter au moins 8 caracteres.");
        }
        utilisateur.setMotDePasseHache(passwordEncoder.encode(nouveau));
        utilisateurRepository.save(utilisateur);
    }

    /** Cas « Se connecter » : identifiant (e-mail ou telephone) + mot de passe. */
    @Transactional
    public Utilisateur connecter(String identifiant, String motDePasseClair) {
        Utilisateur utilisateur = utilisateurRepository.findByTelephone(identifiant)
                .or(() -> utilisateurRepository.findByEmail(identifiant))
                .orElseThrow(() -> new BusinessException("Identifiants incorrects."));
        if (!passwordEncoder.matches(motDePasseClair, utilisateur.getMotDePasseHache())) {
            throw new BusinessException("Identifiants incorrects.");
        }
        if ("BLOQUE".equals(utilisateur.getStatut())) {
            throw new BusinessException("Ce compte est desactive. Contactez l'administrateur.");
        }
        utilisateur.setDerniereConnexion(LocalDateTime.now());
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Cas « Continuer avec Google » : connecte l'utilisateur dont l'e-mail Google
     * existe déjà, sinon crée son compte à la volée puis le connecte. Les deux
     * méthodes coexistent : un compte historique (email + mot de passe) reste
     * utilisable normalement et se voit simplement lié à Google.
     */
    @Transactional
    public Utilisateur connecterAvecGoogle(String uid, String email, String nomComplet, String photoUrl) {
        String emailNorm = email.trim().toLowerCase();
        Optional<Utilisateur> existant = utilisateurRepository.findByEmail(emailNorm);
        if (existant.isPresent()) {
            Utilisateur u = existant.get();
            if ("BLOQUE".equals(u.getStatut())) {
                throw new BusinessException("Ce compte est desactive. Contactez l'administrateur.");
            }
            // Lie le compte existant a Google (sans changer sa methode d'origine).
            if (u.getGoogleUid() == null) {
                u.setGoogleUid(uid);
            }
            u.setDerniereConnexion(LocalDateTime.now());
            return utilisateurRepository.save(u);
        }

        // Nouveau compte Google : on remplit les champs obligatoires avec des
        // valeurs par defaut/placeholder uniques (telephone + CIN), et un mot de
        // passe aleatoire inutilisable. L'utilisateur completera son profil ensuite.
        String[] noms = decouperNom(nomComplet, emailNorm);
        String telephonePlaceholder = genererTelephonePlaceholder();
        Utilisateur u = Utilisateur.builder()
                .prenom(noms[0])
                .nom(noms[1])
                .sexe("N")
                .telephone(telephonePlaceholder)
                .email(emailNorm)
                .adresse("")
                .cinNif("SANS-CIN-" + telephonePlaceholder)
                .dateNaissance(LocalDate.of(2000, 1, 1))
                .motDePasseHache(passwordEncoder.encode("google-" + UUID.randomUUID()))
                .photoUrl(photoUrl)
                .role("MEMBRE")
                .statut("ACTIF")
                .authProvider("GOOGLE")
                .googleUid(uid)
                .build();
        u.setDerniereConnexion(LocalDateTime.now());
        return utilisateurRepository.save(u);
    }

    /** Sépare un nom complet en [prénom, nom] ; à défaut, dérive du courriel. */
    private String[] decouperNom(String nomComplet, String email) {
        String source;
        if (nomComplet != null && !nomComplet.isBlank()) {
            source = nomComplet.trim();
        } else {
            int at = email.indexOf('@');
            source = at > 0 ? email.substring(0, at) : email;
        }
        String[] parts = source.split("\\s+", 2);
        String prenom = parts[0];
        String nom = parts.length > 1 ? parts[1] : parts[0];
        return new String[]{prenom, nom};
    }

    /** Génère un numéro de téléphone placeholder unique pour un compte Google. */
    private String genererTelephonePlaceholder() {
        String tel;
        do {
            StringBuilder sb = new StringBuilder("G");
            for (int i = 0; i < 12; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            tel = sb.toString();
        } while (utilisateurRepository.existsByTelephone(tel));
        return tel;
    }

    /** Cas « Decrire / Modifier son profil ». */
    @Transactional
    public Utilisateur modifierProfil(String utilisateurId, String nom, String prenom,
                                      String adresse, String photoUrl) {
        Utilisateur utilisateur = exiger(utilisateurId);
        if (nom != null && !nom.isBlank()) utilisateur.setNom(nom);
        if (prenom != null && !prenom.isBlank()) utilisateur.setPrenom(prenom);
        if (adresse != null && !adresse.isBlank()) utilisateur.setAdresse(adresse);
        // photoUrl == null : on ne touche pas ; photoUrl vide : on efface la photo.
        if (photoUrl != null) utilisateur.setPhotoUrl(photoUrl.isBlank() ? null : photoUrl);
        return utilisateurRepository.save(utilisateur);
    }

    /** Cas « Activer un compte » (Administrateur). */
    @Transactional
    public Utilisateur activerCompte(String adminId, String utilisateurId) {
        exigerAdmin(adminId);
        Utilisateur utilisateur = exiger(utilisateurId);
        utilisateur.setStatut("ACTIF");
        return utilisateurRepository.save(utilisateur);
    }

    /** Cas « Desactiver un compte » (Administrateur) : impayes repetes, fraude... */
    @Transactional
    public Utilisateur desactiverCompte(String adminId, String utilisateurId) {
        exigerAdmin(adminId);
        Utilisateur utilisateur = exiger(utilisateurId);
        utilisateur.setStatut("BLOQUE");
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Cas « Demander a renouveler son mot de passe » :
     * genere un code a 6 chiffres, valable 15 minutes, a envoyer par SMS.
     */
    @Transactional
    public JetonReinitialisation demanderReinitialisation(String telephone) {
        Utilisateur utilisateur = utilisateurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new BusinessException("Aucun compte pour ce numero."));
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        return jetonRepository.save(JetonReinitialisation.builder()
                .utilisateur(utilisateur)
                .code(code)
                .dateExpiration(LocalDateTime.now().plusMinutes(VALIDITE_JETON_MINUTES))
                .build());
    }

    /** Applique le nouveau mot de passe si le code SMS est valide. */
    @Transactional
    public void reinitialiserMotDePasse(String code, String nouveauMotDePasse) {
        JetonReinitialisation jeton = jetonRepository.findByCodeAndUtiliseFalse(code)
                .orElseThrow(() -> new BusinessException("Code invalide ou deja utilise."));
        if (jeton.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Ce code a expire, demandez-en un nouveau.");
        }
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < 8) {
            throw new BusinessException("Le mot de passe doit compter au moins 8 caracteres.");
        }
        Utilisateur utilisateur = jeton.getUtilisateur();
        utilisateur.setMotDePasseHache(passwordEncoder.encode(nouveauMotDePasse));
        utilisateurRepository.save(utilisateur);
        jeton.setUtilise(true);
        jetonRepository.save(jeton);
    }

    /** Liste tous les utilisateurs (tableau de bord admin). */
    public java.util.List<Utilisateur> listerTous() {
        return utilisateurRepository.findAll();
    }

    private Utilisateur exiger(String id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable : " + id));
    }

    private void exigerAdmin(String adminId) {
        if (!"ADMIN".equals(exiger(adminId).getRole())) {
            throw new BusinessException("Cette action est reservee a l'administrateur.");
        }
    }
}
