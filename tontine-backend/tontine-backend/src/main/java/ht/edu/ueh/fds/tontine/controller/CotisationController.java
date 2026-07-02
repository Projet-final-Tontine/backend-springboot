package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.CotisationResponse;
import ht.edu.ueh.fds.tontine.dto.PaiementResponse;
import ht.edu.ueh.fds.tontine.dto.SolRequests.PayerCotisationRequest;
import ht.edu.ueh.fds.tontine.service.CotisationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints des cotisations et paiements. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CotisationController {

    private final CotisationService cotisationService;

    /** Payer sa cotisation (Membre) avec une reference Mon Cash. */
    @PostMapping("/cotisations/{cotisationId}/payer")
    public PaiementResponse payer(@RequestHeader("X-User-Id") String userId,
                                  @PathVariable String cotisationId,
                                  @RequestBody PayerCotisationRequest req) {
        return PaiementResponse.from(
                cotisationService.payerCotisation(cotisationId, userId, req.referenceMonCash()));
    }

    /** Valider un paiement (Manman sol). */
    @PostMapping("/paiements/{paiementId}/valider")
    public CotisationResponse valider(@RequestHeader("X-User-Id") String userId,
                                      @PathVariable String paiementId) {
        return CotisationResponse.from(cotisationService.validerPaiement(paiementId, userId));
    }

    /** Rejeter un paiement (Manman sol). */
    @PostMapping("/paiements/{paiementId}/rejeter")
    public CotisationResponse rejeter(@RequestHeader("X-User-Id") String userId,
                                      @PathVariable String paiementId) {
        return CotisationResponse.from(cotisationService.rejeterPaiement(paiementId, userId));
    }

    /** Historique des cotisations d'un membre (consulter son compte). */
    @GetMapping("/membres/{membreSolId}/cotisations")
    public List<CotisationResponse> historique(@PathVariable String membreSolId) {
        return cotisationService.historiqueMembre(membreSolId).stream()
                .map(CotisationResponse::from).toList();
    }
}
