package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.KycRequests.KycEtatResponse;
import ht.edu.ueh.fds.tontine.dto.KycRequests.MajIdentiteRequest;
import ht.edu.ueh.fds.tontine.dto.KycRequests.SoumettreKycRequest;
import ht.edu.ueh.fds.tontine.service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Vérification d'identité (KYC) de l'utilisateur connecté : consultation de
 * l'état, confirmation de l'identité, puis soumission des documents.
 */
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;

    /** État courant (identité pré-remplie + statut de vérification). */
    @GetMapping
    public KycEtatResponse etat(Principal principal) {
        return kycService.etat(principal.getName());
    }

    /** Étape 1 : confirme/corrige l'identité. */
    @PutMapping("/identite")
    public KycEtatResponse majIdentite(Principal principal, @RequestBody MajIdentiteRequest req) {
        return kycService.majIdentite(principal.getName(), req.nom(), req.prenom(),
                req.dateNaissance(), req.adresse());
    }

    /** Étape finale : soumission des documents. */
    @PostMapping("/soumettre")
    public KycEtatResponse soumettre(Principal principal, @RequestBody SoumettreKycRequest req) {
        return kycService.soumettre(principal.getName(), req.typeDocument(),
                req.rectoUrl(), req.versoUrl());
    }
}
