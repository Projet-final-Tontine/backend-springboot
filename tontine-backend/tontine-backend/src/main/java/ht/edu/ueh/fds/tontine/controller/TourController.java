package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.SolRequests.*;
import ht.edu.ueh.fds.tontine.dto.TourResponse;
import ht.edu.ueh.fds.tontine.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints des tours de table : ouvrir un tour, payer la main, calendrier. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    /** Declencher (ouvrir) le tour suivant d'un Sol (Manman sol). */
    @PostMapping("/sols/{solId}/tours")
    public ResponseEntity<TourResponse> ouvrir(@RequestHeader("X-User-Id") String userId,
                                               @PathVariable String solId,
                                               @RequestBody OuvrirTourRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TourResponse.from(tourService.ouvrirTour(userId, solId, req.datePrevue())));
    }

    /** Cloturer un tour et verser la main au beneficiaire (Manman sol). */
    @PostMapping("/tours/{tourId}/payer-main")
    public TourResponse payerMain(@RequestHeader("X-User-Id") String userId,
                                  @PathVariable String tourId,
                                  @RequestBody PayerMainRequest req) {
        return TourResponse.from(
                tourService.cloturerEtPayerMain(userId, tourId, req.referenceMonCash()));
    }

    /** Calendrier des tours d'un Sol. */
    @GetMapping("/sols/{solId}/tours")
    public List<TourResponse> calendrier(@PathVariable String solId) {
        return tourService.calendrierDuSol(solId).stream().map(TourResponse::from).toList();
    }
}
