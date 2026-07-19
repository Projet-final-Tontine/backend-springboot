package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.DisponibiliteResponse;
import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.MajUsernameRequest;
import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.RechercheUtilisateurResponse;
import ht.edu.ueh.fds.tontine.dto.UtilisateurResponse;
import ht.edu.ueh.fds.tontine.service.AnnuaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Annuaire public : disponibilité et modification du username, recherche d'un
 * bénéficiaire par username ou e-mail.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UtilisateurController {

    private final AnnuaireService annuaireService;

    /** Vérification de disponibilité (public : utilisé pendant l'inscription). */
    @GetMapping("/username-disponible")
    public DisponibiliteResponse disponibilite(@RequestParam String username) {
        return annuaireService.verifierDisponibilite(username);
    }

    /** Modifie le username de l'utilisateur connecté. */
    @PutMapping("/username")
    public UtilisateurResponse modifierUsername(Principal principal,
                                                @RequestBody MajUsernameRequest req) {
        return UtilisateurResponse.from(
                annuaireService.definirUsername(principal.getName(), req.username()));
    }

    /**
     * Recherche un bénéficiaire par username ou e-mail (détection automatique).
     * Accepte {@code q}, ou {@code username} / {@code email} (compatibilité).
     */
    @GetMapping("/search")
    public RechercheUtilisateurResponse rechercher(
            Principal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        String requete = q != null ? q : (username != null ? username : email);
        return annuaireService.rechercher(requete, principal.getName());
    }
}
