package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.DepotRequest;
import ht.edu.ueh.fds.tontine.dto.PortefeuilleResponse;
import ht.edu.ueh.fds.tontine.service.PortefeuilleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Endpoints du portefeuille (wallet) de l'utilisateur connecte :
 * consultation du solde/historique et depot d'argent.
 */
@RestController
@RequestMapping("/api/portefeuille")
@RequiredArgsConstructor
public class PortefeuilleController {

    private final PortefeuilleService portefeuilleService;

    /** Solde courant + historique des mouvements. */
    @GetMapping
    public PortefeuilleResponse consulter(Principal principal) {
        return portefeuilleService.consulter(principal.getName());
    }

    /** Cas « Deposer de l'argent » : credite le portefeuille (via Mon Cash). */
    @PostMapping("/depot")
    public PortefeuilleResponse deposer(Principal principal, @RequestBody DepotRequest req) {
        portefeuilleService.deposer(principal.getName(), req.montant(), req.referenceMonCash());
        return portefeuilleService.consulter(principal.getName());
    }
}