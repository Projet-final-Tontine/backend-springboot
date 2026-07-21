package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.TransfertDtos.*;
import ht.edu.ueh.fds.tontine.entity.Transfert;
import ht.edu.ueh.fds.tontine.service.TransfertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Transfert d'argent entre utilisateurs : envoi (avec reçu), historique
 * filtrable/recherchable, détail, et page publique de vérification (scannée
 * depuis le QR code du reçu).
 */
@RestController
@RequestMapping("/api/transferts")
@RequiredArgsConstructor
public class TransfertController {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final TransfertService transfertService;

    /** Envoi d'argent ; renvoie le reçu numérique. */
    @PostMapping
    public RecuTransfertResponse transferer(Principal principal, @RequestBody TransfertRequest req) {
        return transfertService.transferer(principal.getName(), req);
    }

    /** Historique (envoyés + reçus), avec filtre et recherche facultatifs. */
    @GetMapping
    public List<TransfertHistoriqueItem> historique(
            Principal principal,
            @RequestParam(required = false) String filtre,
            @RequestParam(required = false) String recherche) {
        return transfertService.historique(principal.getName(), filtre, recherche);
    }

    /** Détail d'un transfert. */
    @GetMapping("/{id}")
    public TransfertDetailResponse detail(Principal principal, @PathVariable String id) {
        return transfertService.detail(principal.getName(), id);
    }

    /** Page publique de vérification d'un reçu (scan du QR). */
    @GetMapping(value = "/verifier/{reference}", produces = MediaType.TEXT_HTML_VALUE)
    public String verifierHtml(@PathVariable String reference) {
        return pageVerification(transfertService.verifier(reference), reference);
    }

    // ------------------------------------------------------------- page HTML

    private String pageVerification(Transfert t, String reference) {
        boolean ok = t != null && "REUSSI".equals(t.getStatut());
        String accent = ok ? "#1B8A4E" : "#C62828";
        String icone = ok ? "✅" : "⚠️";
        String titre = ok ? "Reçu authentique" : (t == null ? "Reçu introuvable" : "Reçu non valide");

        String corps;
        if (t != null) {
            corps = """
                    <div class="ligne"><span>Montant</span><b>%s %s</b></div>
                    <div class="ligne"><span>De</span><b>@%s</b></div>
                    <div class="ligne"><span>À</span><b>@%s</b></div>
                    <div class="ligne"><span>Statut</span><b>%s</b></div>
                    <div class="ligne"><span>Date</span><b>%s</b></div>
                    <div class="ligne"><span>Confirmation</span><b>%s</b></div>
                    <div class="ligne"><span>Transaction</span><b>%s</b></div>
                    """.formatted(
                    new DecimalFormat("#,##0.00").format(t.getMontant()), echapper(t.getDevise()),
                    echapper(t.getExpediteurUsername()), echapper(t.getBeneficiaireUsername()),
                    echapper(t.getStatut()),
                    t.getDateCreation() == null ? "—" : t.getDateCreation().format(FORMAT),
                    echapper(t.getReference()), echapper(t.getTransactionId()));
        } else {
            corps = "<p class=\"msg\">Aucun transfert ne correspond à cette référence.</p>";
        }

        return """
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Vérification du reçu — SOL EN LIGNE</title>
                <style>
                  *{box-sizing:border-box;font-family:-apple-system,Segoe UI,Roboto,sans-serif}
                  body{margin:0;background:linear-gradient(160deg,#3A22A8,#140A38);
                       min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
                  .carte{background:#fff;border-radius:24px;max-width:430px;width:100%%;
                         padding:32px;box-shadow:0 20px 60px rgba(0,0,0,.35)}
                  .badge{width:72px;height:72px;border-radius:50%%;margin:0 auto 14px;
                         display:flex;align-items:center;justify-content:center;font-size:38px;background:%s22}
                  h1{text-align:center;color:%s;font-size:22px;margin:0 0 4px}
                  .sous{text-align:center;color:#6B7280;font-size:13px;margin-bottom:22px}
                  .ligne{display:flex;justify-content:space-between;padding:12px 0;
                         border-bottom:1px solid #EEE;font-size:15px;color:#333}
                  .ligne span{color:#6B7280}
                  .msg{text-align:center;color:#C62828;font-size:15px}
                  .logo{text-align:center;font-weight:800;color:#3A22A8;letter-spacing:1px;margin-bottom:18px}
                  .pied{text-align:center;color:#9CA3AF;font-size:12px;margin-top:20px}
                </style></head>
                <body><div class="carte">
                  <div class="logo">SOL EN LIGNE</div>
                  <div class="badge">%s</div>
                  <h1>%s</h1>
                  <div class="sous">Vérification d'un reçu de transfert</div>
                  %s
                  <div class="pied">Vérification officielle · %s</div>
                </div></body></html>
                """.formatted(accent, accent, icone, titre, corps, echapper(reference));
    }

    private String echapper(String s) {
        if (s == null) return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
