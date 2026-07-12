package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.SondageRequests;
import ht.edu.ueh.fds.tontine.dto.SondageResponse;
import ht.edu.ueh.fds.tontine.service.SondageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/** Endpoints des sondages / votes de groupe. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SondageController {

    private final SondageService sondageService;

    /** Creer un sondage dans un Sol (membre). */
    @PostMapping("/sols/{solId}/sondages")
    public SondageResponse creer(Principal principal, @PathVariable String solId,
                                 @RequestBody SondageRequests.CreerSondageRequest req) {
        return sondageService.creer(principal.getName(), solId, req.question(), req.options());
    }

    /** Liste des sondages d'un Sol (membre). */
    @GetMapping("/sols/{solId}/sondages")
    public List<SondageResponse> lister(Principal principal, @PathVariable String solId) {
        return sondageService.duSol(principal.getName(), solId);
    }

    /** Voter pour une option d'un sondage. */
    @PostMapping("/sondages/{sondageId}/voter")
    public SondageResponse voter(Principal principal, @PathVariable String sondageId,
                                 @RequestBody SondageRequests.VoterRequest req) {
        return sondageService.voter(principal.getName(), sondageId, req.optionIndex());
    }

    /** Clore un sondage (createur ou Manman sol). */
    @PostMapping("/sondages/{sondageId}/cloturer")
    public SondageResponse cloturer(Principal principal, @PathVariable String sondageId) {
        return sondageService.cloturer(principal.getName(), sondageId);
    }
}
