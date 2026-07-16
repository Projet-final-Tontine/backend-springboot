package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.FonSekouRequests.*;
import ht.edu.ueh.fds.tontine.dto.FonSekouResponse;
import ht.edu.ueh.fds.tontine.service.FonSekouService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/** Endpoints du Fon Sekou : caisse de solidarité, demandes de secours et votes. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FonSekouController {

    private final FonSekouService fonSekouService;

    /** État de la caisse + demandes d'un Sol. */
    @GetMapping("/sols/{solId}/fon-sekou")
    public FonSekouResponse etat(Principal principal, @PathVariable String solId) {
        return fonSekouService.etat(principal.getName(), solId);
    }

    /** Alimenter la caisse (depuis son portefeuille). */
    @PostMapping("/sols/{solId}/fon-sekou/contribuer")
    public FonSekouResponse contribuer(Principal principal, @PathVariable String solId,
                                       @RequestBody ContribuerRequest req) {
        return fonSekouService.contribuer(principal.getName(), solId, req.montant());
    }

    /** Déposer une demande de secours. */
    @PostMapping("/sols/{solId}/fon-sekou/demandes")
    public FonSekouResponse demander(Principal principal, @PathVariable String solId,
                                     @RequestBody DemanderSekouRequest req) {
        return fonSekouService.demander(principal.getName(), solId,
                req.type(), req.montant(), req.motif(), req.justificatifUrl());
    }

    /** Voter pour ou contre une demande de secours. */
    @PostMapping("/fon-sekou/demandes/{demandeId}/voter")
    public FonSekouResponse voter(Principal principal, @PathVariable String demandeId,
                                  @RequestBody VoterSekouRequest req) {
        return fonSekouService.voter(principal.getName(), demandeId, req.pour());
    }

    /** Clôturer une demande (Manman sol) : versement si le vote est favorable. */
    @PostMapping("/fon-sekou/demandes/{demandeId}/cloturer")
    public FonSekouResponse cloturer(Principal principal, @PathVariable String demandeId) {
        return fonSekouService.cloturer(principal.getName(), demandeId);
    }
}
