package ht.edu.ueh.fds.tontine.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Vérifie les jetons d'identité Firebase (connexion « Continuer avec Google »).
 *
 * L'initialisation de Firebase est <b>paresseuse et optionnelle</b> : elle n'a
 * lieu qu'au premier appel réel. Si les identifiants Firebase ne sont pas
 * configurés, l'endpoint Google renvoie une erreur claire, mais le reste de
 * l'application (inscription/connexion classiques) démarre et fonctionne
 * normalement — la nouvelle méthode n'impacte donc jamais l'existant.
 */
@Service
public class FirebaseService {

    /** Chemin du fichier de clé de service (JSON). Vide = tenter les identifiants par défaut. */
    @Value("${app.firebase.credentials-path:}")
    private String cheminIdentifiants;

    /** Identifiant du projet Firebase (facultatif si présent dans la clé). */
    @Value("${app.firebase.project-id:}")
    private String projetId;

    private volatile FirebaseAuth authCache;
    private volatile boolean initEssayee;

    /** Infos utiles extraites du jeton Firebase vérifié. */
    public record InfosGoogle(String uid, String email, String nomComplet, String photoUrl) {
    }

    /**
     * Vérifie un jeton d'identité Firebase et renvoie les informations du compte.
     * Lève une {@link BusinessException} si Firebase n'est pas configuré ou si le
     * jeton est invalide/expiré.
     */
    public InfosGoogle verifier(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException("Jeton Google manquant.");
        }
        FirebaseToken jeton;
        try {
            jeton = auth().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new BusinessException("Jeton Google invalide ou expiré.");
        }
        if (jeton.getEmail() == null || jeton.getEmail().isBlank()) {
            throw new BusinessException("Ce compte Google ne fournit pas d'adresse e-mail.");
        }
        Object photo = jeton.getClaims().get("picture");
        return new InfosGoogle(
                jeton.getUid(),
                jeton.getEmail().toLowerCase(),
                jeton.getName(),
                photo == null ? null : photo.toString());
    }

    /** Initialise Firebase à la demande (une seule fois). */
    private synchronized FirebaseAuth auth() {
        if (authCache != null) {
            return authCache;
        }
        if (initEssayee) {
            throw new BusinessException("La connexion Google n'est pas configurée sur le serveur.");
        }
        initEssayee = true;
        try {
            GoogleCredentials identifiants;
            if (cheminIdentifiants != null && !cheminIdentifiants.isBlank()) {
                try (InputStream in = new FileInputStream(cheminIdentifiants)) {
                    identifiants = GoogleCredentials.fromStream(in);
                }
            } else {
                // Variable d'env GOOGLE_APPLICATION_CREDENTIALS, le cas échéant.
                identifiants = GoogleCredentials.getApplicationDefault();
            }
            FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(identifiants);
            if (projetId != null && !projetId.isBlank()) {
                options.setProjectId(projetId);
            }
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options.build())
                    : FirebaseApp.getInstance();
            authCache = FirebaseAuth.getInstance(app);
            return authCache;
        } catch (Exception e) {
            throw new BusinessException("La connexion Google n'est pas configurée sur le serveur.");
        }
    }
}
