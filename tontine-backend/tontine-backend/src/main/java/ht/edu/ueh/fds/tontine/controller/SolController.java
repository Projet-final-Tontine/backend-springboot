package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.MembreSolResponse;
import ht.edu.ueh.fds.tontine.dto.SolRequests.*;
import ht.edu.ueh.fds.tontine.dto.SolResponse;
import ht.edu.ueh.fds.tontine.entity.Sol;
import ht.edu.ueh.fds.tontine.service.SolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints du cercle de tontine : creer, rejoindre, demarrer, membres. */
@RestController
@RequestMapping("/api/sols")
@RequiredArgsConstructor
public class SolController {

    private final SolService solService;

    /** Creer un Sol (Manman sol). */
    @PostMapping
    public ResponseEntity<SolResponse> creer(@RequestHeader("X-User-Id") String userId,
                                             @RequestBody CreerSolRequest req) {
        Sol sol = Sol.builder()
                .nom(req.nom()).description(req.description())
                .nombreMaxMembres(req.nombreMaxMembres())
                .montantCotisation(req.montantCotisation())
                .frequence(req.frequence()).dateDebut(req.dateDebut())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SolResponse.from(solService.creerSol(userId, sol)));
    }

    /** Rejoindre un Sol via le code d'invitation. */
    @PostMapping("/rejoindre")
    public ResponseEntity<MembreSolResponse> rejoindre(@RequestHeader("X-User-Id") String userId,
                                                       @RequestBody RejoindreRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MembreSolResponse.from(solService.rejoindreParCode(userId, req.codeInvitation())));
    }

    /** Demarrer le cycle (plus d'adhesion possible). */
    @PostMapping("/{solId}/demarrer")
    public SolResponse demarrer(@RequestHeader("X-User-Id") String userId,
                                @PathVariable String solId) {
        return SolResponse.from(solService.demarrerCycle(userId, solId));
    }

    /** Quitter un Sol. */
    @DeleteMapping("/{solId}/membres/moi")
    public ResponseEntity<Void> seDesinscrire(@RequestHeader("X-User-Id") String userId,
                                              @PathVariable String solId) {
        solService.seDesinscrire(userId, solId);
        return ResponseEntity.noContent().build();
    }

    /** Liste des membres d'un Sol, dans l'ordre de la rotation. */
    @GetMapping("/{solId}/membres")
    public List<MembreSolResponse> membres(@PathVariable String solId) {
        return solService.membresDuSol(solId).stream().map(MembreSolResponse::from).toList();
    }
}
