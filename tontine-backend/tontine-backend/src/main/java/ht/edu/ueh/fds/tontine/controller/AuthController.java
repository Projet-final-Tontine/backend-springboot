package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.AuthRequests.*;
import ht.edu.ueh.fds.tontine.dto.ConnexionResponse;
import ht.edu.ueh.fds.tontine.dto.UtilisateurResponse;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.security.FirebaseService;
import ht.edu.ueh.fds.tontine.security.JwtService;
import ht.edu.ueh.fds.tontine.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/** Endpoints de comptes : inscription, connexion, profil, mot de passe. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilisateurService utilisateurService;
    private final JwtService jwtService;
    private final FirebaseService firebaseService;

    @PostMapping("/inscription")
    public ResponseEntity<UtilisateurResponse> inscrire(@RequestBody InscriptionRequest req) {
        Utilisateur u = Utilisateur.builder()
                .nom(req.nom()).prenom(req.prenom()).sexe(req.sexe())
                .telephone(req.telephone()).email(req.email()).adresse(req.adresse())
                .cinNif(req.cinNif()).dateNaissance(req.dateNaissance())
                .role(req.role())
                .build();
        Utilisateur cree = utilisateurService.inscrire(u, req.motDePasse());
        return ResponseEntity.status(HttpStatus.CREATED).body(UtilisateurResponse.from(cree));
    }

    /** Connexion : renvoie un jeton JWT a utiliser pour les requetes suivantes. */
    @PostMapping("/connexion")
    public ConnexionResponse connecter(@RequestBody ConnexionRequest req) {
        Utilisateur u = utilisateurService.connecter(req.telephone(), req.motDePasse());
        String token = jwtService.genererToken(u);
        return new ConnexionResponse(token, UtilisateurResponse.from(u));
    }

    /**
     * Connexion « Continuer avec Google » : vérifie le jeton Firebase, connecte
     * le compte lié à cet e-mail (ou le crée), puis renvoie un jeton JWT de
     * l'application — exactement comme la connexion classique.
     */
    @PostMapping("/google")
    public ConnexionResponse connecterGoogle(@RequestBody GoogleAuthRequest req) {
        FirebaseService.InfosGoogle infos = firebaseService.verifier(req.idToken());
        Utilisateur u = utilisateurService.connecterAvecGoogle(
                infos.uid(), infos.email(), infos.nomComplet(), infos.photoUrl());
        String token = jwtService.genererToken(u);
        return new ConnexionResponse(token, UtilisateurResponse.from(u));
    }

    /** Modifier son profil (l'utilisateur est identifie par son jeton). */
    @PutMapping("/profil")
    public UtilisateurResponse modifierProfil(Principal principal,
                                              @RequestBody ModifierProfilRequest req) {
        return UtilisateurResponse.from(utilisateurService.modifierProfil(
                principal.getName(), req.nom(), req.prenom(), req.adresse(), req.photoUrl()));
    }

    /** Changer son mot de passe (utilisateur connecte, ancien mot de passe requis). */
    @PostMapping("/changer-mot-de-passe")
    public Map<String, String> changerMotDePasse(Principal principal,
                                                 @RequestBody ChangerMotDePasseRequest req) {
        utilisateurService.changerMotDePasse(
                principal.getName(), req.ancienMotDePasse(), req.nouveauMotDePasse());
        return Map.of("message", "Mot de passe mis a jour avec succes.");
    }

    @PostMapping("/mot-de-passe/demande")
    public Map<String, String> demanderReset(@RequestBody DemandeResetRequest req) {
        utilisateurService.demanderReinitialisation(req.telephone());
        return Map.of("message", "Si le numero existe, un code de reinitialisation a ete envoye par SMS.");
    }

    @PostMapping("/mot-de-passe/reset")
    public Map<String, String> reinitialiser(@RequestBody ResetRequest req) {
        utilisateurService.reinitialiserMotDePasse(req.code(), req.nouveauMotDePasse());
        return Map.of("message", "Mot de passe mis a jour avec succes.");
    }
}
