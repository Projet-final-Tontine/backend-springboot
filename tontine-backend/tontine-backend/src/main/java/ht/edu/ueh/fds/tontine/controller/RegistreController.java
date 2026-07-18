package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.BlocRegistreResponse;
import ht.edu.ueh.fds.tontine.dto.VerificationRegistreResponse;
import ht.edu.ueh.fds.tontine.service.RegistreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Registre Inviolable : consultation du grand livre scellé, vérification
 * d'intégrité pour l'application, et page HTML publique d'intégrité (un tiers
 * — auditeur, banque — constate en un coup d'œil que la comptabilité n'a pas
 * été altérée).
 */
@RestController
@RequestMapping("/api/registre")
@RequiredArgsConstructor
public class RegistreController {

    private final RegistreService registreService;

    /** Liste des blocs scellés (du plus récent au plus ancien). */
    @GetMapping
    public List<BlocRegistreResponse> registre() {
        return registreService.lister();
    }

    /** Vérifie l'intégrité de toute la chaîne (badge « inviolable » dans l'app). */
    @GetMapping("/verifier")
    public VerificationRegistreResponse verifier() {
        return registreService.verifier();
    }

    /** Page HTML publique d'attestation d'intégrité du registre. */
    @GetMapping(value = "/integrite", produces = MediaType.TEXT_HTML_VALUE)
    public String integriteHtml() {
        return pageIntegrite(registreService.verifier());
    }

    // ------------------------------------------------------------- page HTML

    private String pageIntegrite(VerificationRegistreResponse v) {
        boolean ok = v.intacte();
        String accent = ok ? "#1B8A4E" : "#C62828";
        String icone = ok ? "🔒" : "⚠️";
        String titre = ok ? "Registre intègre" : "Registre altéré";

        String corps;
        if (ok) {
            String empreinte = v.empreinteGlobale() == null ? "—" : v.empreinteGlobale();
            corps = """
                    <div class="ligne"><span>Écritures scellées</span><b>%d</b></div>
                    <div class="ligne"><span>État</span><b style="color:%s">Authentique ✅</b></div>
                    <div class="bloc"><span>Empreinte globale (SHA-256)</span>
                        <code>%s</code></div>
                    """.formatted(v.nombreBlocs(), accent, echapper(empreinte));
        } else {
            corps = """
                    <div class="ligne"><span>Bloc corrompu</span><b>#%s</b></div>
                    <p class="msg">%s</p>
                    """.formatted(v.positionRupture() == null ? "?" : v.positionRupture(),
                    echapper(v.message()));
        }

        return """
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Intégrité du registre — SOL EN LIGNE</title>
                <style>
                  *{box-sizing:border-box;font-family:-apple-system,Segoe UI,Roboto,sans-serif}
                  body{margin:0;background:linear-gradient(160deg,#3A22A8,#140A38);
                       min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px}
                  .carte{background:#fff;border-radius:24px;max-width:460px;width:100%%;
                         padding:32px;box-shadow:0 20px 60px rgba(0,0,0,.35)}
                  .badge{width:72px;height:72px;border-radius:50%%;margin:0 auto 14px;
                         display:flex;align-items:center;justify-content:center;font-size:38px;
                         background:%s22}
                  h1{text-align:center;color:%s;font-size:22px;margin:0 0 4px}
                  .sous{text-align:center;color:#6B7280;font-size:13px;margin-bottom:22px}
                  .ligne{display:flex;justify-content:space-between;padding:12px 0;
                         border-bottom:1px solid #EEE;font-size:15px;color:#333}
                  .ligne span{color:#6B7280}
                  .bloc{padding:12px 0;font-size:13px;color:#6B7280}
                  .bloc code{display:block;margin-top:6px;word-break:break-all;color:#111;
                             font-family:monospace;font-size:12px;background:#F4F4F8;
                             padding:10px;border-radius:8px}
                  .msg{text-align:center;color:#C62828;font-size:15px;line-height:1.5}
                  .pied{text-align:center;color:#9CA3AF;font-size:12px;margin-top:20px;line-height:1.5}
                  .logo{text-align:center;font-weight:800;color:#3A22A8;letter-spacing:1px;margin-bottom:18px}
                </style></head>
                <body><div class="carte">
                  <div class="logo">SOL EN LIGNE</div>
                  <div class="badge">%s</div>
                  <h1>%s</h1>
                  <div class="sous">Attestation d'intégrité du Registre Inviolable</div>
                  %s
                  <div class="pied">Chaque mouvement d'argent est scellé par un hash SHA-256
                     chaîné au précédent.<br>Toute modification d'une écriture passée est
                     mathématiquement détectable.</div>
                </div></body></html>
                """.formatted(accent, accent, icone, titre, corps);
    }

    /** Échappe le HTML pour éviter toute injection dans la page publique. */
    private String echapper(String s) {
        if (s == null) return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
