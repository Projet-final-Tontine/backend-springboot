package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.EnvoyerMessageRequest;
import ht.edu.ueh.fds.tontine.dto.MessageRecentResponse;
import ht.edu.ueh.fds.tontine.dto.MessageResponse;
import ht.edu.ueh.fds.tontine.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/** Endpoints de la messagerie : chat de groupe d'un Sol et chat prive. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // ----- Chat de groupe -----

    @GetMapping("/sols/{solId}/messages")
    public List<MessageResponse> messagesSol(Principal principal, @PathVariable String solId) {
        return messageService.messagesDuSol(principal.getName(), solId);
    }

    @PostMapping("/sols/{solId}/messages")
    public MessageResponse envoyerAuSol(Principal principal, @PathVariable String solId,
                                        @RequestBody EnvoyerMessageRequest req) {
        return messageService.envoyerAuSol(principal.getName(), solId,
                req.contenu(), req.pieceJointeUrl(), req.typePiece());
    }

    // ----- Chat prive -----

    @GetMapping("/messages/prive/{autreId}")
    public List<MessageResponse> conversationPrivee(Principal principal, @PathVariable String autreId) {
        return messageService.conversationPrivee(principal.getName(), autreId);
    }

    @PostMapping("/messages/prive/{destinataireId}")
    public MessageResponse envoyerPrive(Principal principal, @PathVariable String destinataireId,
                                        @RequestBody EnvoyerMessageRequest req) {
        return messageService.envoyerPrive(principal.getName(), destinataireId,
                req.contenu(), req.pieceJointeUrl(), req.typePiece());
    }

    // ----- Notifications : messages recents destines a l'utilisateur -----

    @GetMapping("/messages/recents")
    public List<MessageRecentResponse> messagesRecents(
            Principal principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime depuis) {
        LocalDateTime borne = depuis != null ? depuis : LocalDateTime.now().minusDays(1);
        return messageService.messagesRecents(principal.getName(), borne);
    }
}
