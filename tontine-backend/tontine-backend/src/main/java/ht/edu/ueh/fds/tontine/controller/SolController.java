package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.MembreSolResponse;
import ht.edu.ueh.fds.tontine.dto.SolDetailResponse;
import ht.edu.ueh.fds.tontine.dto.SolRequests.*;
import ht.edu.ueh.fds.tontine.dto.SolResponse;
import ht.edu.ueh.fds.tontine.entity.Sol;
import ht.edu.ueh.fds.tontine.service.SolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/** Endpoints du cercle de tontine : creer, rejoindre, demarrer, membres. */
@RestController
@RequestMapping("/api/sols")
@RequiredArgsConstructor
public class SolController {

    private final SolService solService;

    /** Creer un Sol (Manman sol). */
    @PostMapping
    public ResponseEntity<SolResponse> creer(Principal principal,
                                             @RequestBody CreerSolRequest req) {
        Sol sol = Sol.builder()
                .nom(req.nom()).description(req.description())
                .nombreMaxMembres(req.nombreMaxMembres())
                .montantCotisation(req.montantCotisation())
                .frequence(req.frequence()).dateDebut(req.dateDebut())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SolResponse.from(solService.creerSol(principal.getName(), sol)));
    }

    /** Liste des Sols de l'utilisateur connecte. */
    @GetMapping("/mes-sols")
    public List<SolResponse> mesSols(Principal principal) {
        return solService.solsDeLUtilisateur(principal.getName());
    }

    /** Detail complet d'un Sol : progression, tour en cours, cotisations, position. */
    @GetMapping("/{solId}/detail")
    public SolDetailResponse detail(Principal principal, @PathVariable String solId) {
        return solService.detailDuSol(principal.getName(), solId);
    }

    /** Tous les tours (distributions) des Sols de l'utilisateur — calendrier intelligent. */
    @GetMapping("/mes-tours")
    public List<ht.edu.ueh.fds.tontine.dto.MonTourResponse> mesTours(Principal principal) {
        return solService.toursDeLUtilisateur(principal.getName());
    }

    /** Rejoindre un Sol via le code d'invitation. */
    @PostMapping("/rejoindre")
    public ResponseEntity<MembreSolResponse> rejoindre(Principal principal,
                                                       @RequestBody RejoindreRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MembreSolResponse.from(
                        solService.rejoindreParCode(principal.getName(), req.codeInvitation())));
    }

    /** Demarrer le cycle (plus d'adhesion possible). */
    @PostMapping("/{solId}/demarrer")
    public SolResponse demarrer(Principal principal, @PathVariable String solId) {
        return solService.demarrerCycle(principal.getName(), solId);
    }

    /** Quitter un Sol. */
    @DeleteMapping("/{solId}/membres/moi")
    public ResponseEntity<Void> seDesinscrire(Principal principal, @PathVariable String solId) {
        solService.seDesinscrire(principal.getName(), solId);
        return ResponseEntity.noContent().build();
    }

    /** Liste des membres d'un Sol, dans l'ordre de la rotation. */
    @GetMapping("/{solId}/membres")
    public List<MembreSolResponse> membres(@PathVariable String solId) {
        return solService.membresDuSol(solId);
    }
}
