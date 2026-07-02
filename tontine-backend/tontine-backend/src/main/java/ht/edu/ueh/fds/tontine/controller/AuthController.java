package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.AuthRequests.*;
import ht.edu.ueh.fds.tontine.dto.UtilisateurResponse;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Endpoints de comptes : inscription, connexion, profil, mot de passe. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UtilisateurService utilisateurService;

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

    @PostMapping("/connexion")
    public UtilisateurResponse connecter(@RequestBody ConnexionRequest req) {
        return UtilisateurResponse.from(
                utilisateurService.connecter(req.telephone(), req.motDePasse()));
    }

    @PutMapping("/profil")
    public UtilisateurResponse modifierProfil(@RequestHeader("X-User-Id") String userId,
                                              @RequestBody ModifierProfilRequest req) {
        return UtilisateurResponse.from(utilisateurService.modifierProfil(
                userId, req.nom(), req.prenom(), req.adresse(), req.photoUrl()));
    }

    @PostMapping("/mot-de-passe/demande")
    public Map<String, String> demanderReset(@RequestBody DemandeResetRequest req) {
        utilisateurService.demanderReinitialisation(req.telephone());
        // Le code est envoye par SMS ; on ne le renvoie jamais dans la reponse.
        return Map.of("message", "Si le numero existe, un code de reinitialisation a ete envoye par SMS.");
    }

    @PostMapping("/mot-de-passe/reset")
    public Map<String, String> reinitialiser(@RequestBody ResetRequest req) {
        utilisateurService.reinitialiserMotDePasse(req.code(), req.nouveauMotDePasse());
        return Map.of("message", "Mot de passe mis a jour avec succes.");
    }
}
