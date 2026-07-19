package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.PortefeuilleResponse;
import ht.edu.ueh.fds.tontine.service.TransfertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;

/** Transfert d'argent entre utilisateurs (par username ou e-mail). */
@RestController
@RequestMapping("/api/transferts")
@RequiredArgsConstructor
public class TransfertController {

    private final TransfertService transfertService;

    public record TransfertRequest(String beneficiaire, BigDecimal montant, String note) {
    }

    @PostMapping
    public PortefeuilleResponse transferer(Principal principal, @RequestBody TransfertRequest req) {
        return transfertService.transferer(principal.getName(),
                req.beneficiaire(), req.montant(), req.note());
    }
}
