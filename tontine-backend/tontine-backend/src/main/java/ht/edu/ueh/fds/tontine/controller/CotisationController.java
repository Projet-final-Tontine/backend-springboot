package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.CotisationResponse;
import ht.edu.ueh.fds.tontine.dto.PaiementResponse;
import ht.edu.ueh.fds.tontine.dto.SolRequests.PayerCotisationRequest;
import ht.edu.ueh.fds.tontine.service.CotisationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/** Endpoints des cotisations et paiements. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CotisationController {

    private final CotisationService cotisationService;

    /** Payer sa cotisation (Membre) avec une reference Mon Cash. */
    @PostMapping("/cotisations/{cotisationId}/payer")
    public PaiementResponse payer(Principal principal,
                                  @PathVariable String cotisationId,
                                  @RequestBody PayerCotisationRequest req) {
        return PaiementResponse.from(
                cotisationService.payerCotisation(cotisationId, principal.getName(), req.referenceMonCash()));
    }

    /** Valider un paiement (Manman sol). */
    @PostMapping("/paiements/{paiementId}/valider")
    public CotisationResponse valider(Principal principal, @PathVariable String paiementId) {
        return CotisationResponse.from(cotisationService.validerPaiement(paiementId, principal.getName()));
    }

    /** Rejeter un paiement (Manman sol). */
    @PostMapping("/paiements/{paiementId}/rejeter")
    public CotisationResponse rejeter(Principal principal, @PathVariable String paiementId) {
        return CotisationResponse.from(cotisationService.rejeterPaiement(paiementId, principal.getName()));
    }

    /** Historique des cotisations d'un membre (consulter son compte). */
    @GetMapping("/membres/{membreSolId}/cotisations")
    public List<CotisationResponse> historique(@PathVariable String membreSolId) {
        return cotisationService.historiqueMembre(membreSolId).stream()
                .map(CotisationResponse::from).toList();
    }
}
