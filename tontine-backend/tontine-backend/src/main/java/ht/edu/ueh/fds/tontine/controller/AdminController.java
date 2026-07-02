package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.SolRequests.*;
import ht.edu.ueh.fds.tontine.dto.UtilisateurResponse;
import ht.edu.ueh.fds.tontine.service.GarantieService;
import ht.edu.ueh.fds.tontine.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * Endpoints reserves a l'Administrateur : comptes et contrepartie.
 * L'acces est deja restreint au role ADMIN par la configuration de securite.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UtilisateurService utilisateurService;
    private final GarantieService garantieService;

    /** Activer un compte utilisateur. */
    @PostMapping("/utilisateurs/{cibleId}/activer")
    public UtilisateurResponse activer(Principal principal, @PathVariable String cibleId) {
        return UtilisateurResponse.from(utilisateurService.activerCompte(principal.getName(), cibleId));
    }

    /** Desactiver un compte (impayes repetes, fraude). */
    @PostMapping("/utilisateurs/{cibleId}/desactiver")
    public UtilisateurResponse desactiver(Principal principal, @PathVariable String cibleId) {
        return UtilisateurResponse.from(utilisateurService.desactiverCompte(principal.getName(), cibleId));
    }

    /** Alimenter la caisse de garantie d'un Sol. */
    @PostMapping("/garantie/caisses/{solId}/alimenter")
    public Map<String, Object> alimenter(Principal principal,
                                         @PathVariable String solId,
                                         @RequestBody AlimenterCaisseRequest req) {
        var caisse = garantieService.alimenterCaisse(principal.getName(), solId, req.montant());
        return Map.of("solId", solId, "nouveauSolde", caisse.getSolde());
    }

    /** Activer la contrepartie pour couvrir une cotisation impayee. */
    @PostMapping("/garantie/contrepartie")
    public Map<String, Object> activerContrepartie(Principal principal,
                                                   @RequestBody ActiverContrepartieRequest req) {
        var activation = garantieService.activerContrepartie(
                principal.getName(), req.cotisationId(), req.motif());
        return Map.of(
                "activationId", activation.getId(),
                "montant", activation.getMontant(),
                "message", "Contrepartie activee : le membre est marque defaillant.");
    }

    /** Enregistrer le remboursement d'une garantie par le membre defaillant. */
    @PostMapping("/garantie/activations/{activationId}/rembourser")
    public Map<String, Object> rembourser(Principal principal, @PathVariable String activationId) {
        var activation = garantieService.rembourserGarantie(principal.getName(), activationId);
        return Map.of("activationId", activation.getId(), "rembourse", activation.getRembourse());
    }
}
