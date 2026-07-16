package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.ReleveResponse;
import ht.edu.ueh.fds.tontine.dto.VerificationReleveResponse;
import ht.edu.ueh.fds.tontine.service.ReleveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.format.DateTimeFormatter;

/**
 * Certificat de Fiabilité Financière : génération pour le membre connecté et
 * page publique de vérification (scannée depuis le QR code par une banque).
 */
@RestController
@RequestMapping("/api/releve")
@RequiredArgsConstructor
public class ReleveController {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final ReleveService releveService;

    /** Génère le relevé de fiabilité de l'utilisateur connecté. */
    @GetMapping
    public ReleveResponse monReleve(Principal principal) {
        return releveService.genererReleve(principal.getName());
    }

    /**
     * Vérification publique d'un relevé (scan du QR). Renvoie une page HTML
     * lisible : un tiers voit immédiatement si le document est authentique.
     */
    @GetMapping(value = "/verifier/{reference}", produces = MediaType.TEXT_HTML_VALUE)
    public String verifierHtml(@PathVariable String reference) {
        return pageVerification(releveService.verifier(reference), reference);
    }

    // ------------------------------------------------------------- page HTML

    private String pageVerification(VerificationReleveResponse v, String reference) {
        boolean ok = v.authentique();
        String accent = ok ? "#1B8A4E" : "#C62828";
        String icone = ok ? "✅" : "⚠️";
        String titre = ok ? "Relevé authentique" : "Document non reconnu";

        String corps;
        if (ok) {
            String date = v.dateEmission() == null ? "—" : v.dateEmission().format(FORMAT);
            corps = """
                    <div class="ligne"><span>Titulaire</span><b>%s</b></div>
                    <div class="ligne"><span>Score de fiabilité</span><b>%d / 100</b></div>
                    <div class="ligne"><span>Note</span><b>%s (%s)</b></div>
                    <div class="ligne"><span>Émis le</span><b>%s</b></div>
                    <div class="ligne"><span>Référence</span><b>%s</b></div>
                    """.formatted(echapper(v.nomComplet()), v.scoreGlobal(),
                    v.note(), echapper(v.niveau()), date, echapper(reference));
        } else {
            corps = "<p class=\"msg\">%s</p>".formatted(echapper(v.message()));
        }

        return """
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Vérification — SOL EN LIGNE</title>
                <style>
                  *{box-sizing:border-box;font-family:-apple-system,Segoe UI,Roboto,sans-serif}
                  body{margin:0;background:linear-gradient(160deg,#3A22A8,#140A38);
                       min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
                  .carte{background:#fff;border-radius:24px;max-width:420px;width:100%%;
                         padding:32px;box-shadow:0 20px 60px rgba(0,0,0,.35)}
                  .badge{width:72px;height:72px;border-radius:50%%;margin:0 auto 14px;
                         display:flex;align-items:center;justify-content:center;font-size:38px;
                         background:%s22}
                  h1{text-align:center;color:%s;font-size:22px;margin:0 0 4px}
                  .sous{text-align:center;color:#6B7280;font-size:13px;margin-bottom:22px}
                  .ligne{display:flex;justify-content:space-between;padding:12px 0;
                         border-bottom:1px solid #EEE;font-size:15px;color:#333}
                  .ligne span{color:#6B7280}
                  .msg{text-align:center;color:#C62828;font-size:15px;line-height:1.5}
                  .pied{text-align:center;color:#9CA3AF;font-size:12px;margin-top:20px}
                  .logo{text-align:center;font-weight:800;color:#3A22A8;letter-spacing:1px;margin-bottom:18px}
                </style></head>
                <body><div class="carte">
                  <div class="logo">SOL EN LIGNE</div>
                  <div class="badge">%s</div>
                  <h1>%s</h1>
                  <div class="sous">Vérification du Certificat de Fiabilité Financière</div>
                  %s
                  <div class="pied">Vérification officielle · %s</div>
                </div></body></html>
                """.formatted(accent, accent, icone, titre, corps, echapper(reference));
    }

    /** Échappe le HTML pour éviter toute injection dans la page de vérification. */
    private String echapper(String s) {
        if (s == null) return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
