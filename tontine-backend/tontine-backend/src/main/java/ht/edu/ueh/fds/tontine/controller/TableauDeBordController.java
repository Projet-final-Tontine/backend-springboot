package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.TableauDeBordResponse;
import ht.edu.ueh.fds.tontine.service.TableauDeBordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/** Vue consolidée « Mon activité » pour l'écran d'accueil. */
@RestController
@RequestMapping("/api/tableau-de-bord")
@RequiredArgsConstructor
public class TableauDeBordController {

    private final TableauDeBordService tableauDeBordService;

    @GetMapping
    public TableauDeBordResponse tableauDeBord(Principal principal) {
        return tableauDeBordService.construire(principal.getName());
    }
}
