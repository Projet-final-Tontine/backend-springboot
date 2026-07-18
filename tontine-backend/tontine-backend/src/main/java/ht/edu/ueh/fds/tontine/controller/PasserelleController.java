package ht.edu.ueh.fds.tontine.controller;

import ht.edu.ueh.fds.tontine.dto.PaiementRequests.InitierPaiementRequest;
import ht.edu.ueh.fds.tontine.dto.PaiementRequests.InitierPaiementResponse;
import ht.edu.ueh.fds.tontine.dto.PaiementRequests.StatutPaiementResponse;
import ht.edu.ueh.fds.tontine.entity.PaiementSimule;
import ht.edu.ueh.fds.tontine.service.PasserelleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.text.DecimalFormat;

/**
 * Passerelle de paiement : initialisation depuis l'application (protégée par
 * jeton), pages web de paiement de marque (Mon Cash, NatCash, cartes…) ouvertes
 * dans le navigateur (publiques, protégées par l'identifiant d'ordre imprévisible),
 * et interrogation de l'état au retour dans l'application.
 */
@RestController
@RequiredArgsConstructor
public class PasserelleController {

    private final PasserelleService passerelleService;

    // ------------------------------------------------------------- API mobile

    /** Crée un ordre de paiement ; l'application ouvrira l'URL renvoyée. */
    @PostMapping("/api/portefeuille/paiement/initier")
    public InitierPaiementResponse initier(Principal principal,
                                           @RequestBody InitierPaiementRequest req) {
        return passerelleService.initier(principal.getName(), req.sens(),
                req.moyen(), req.montant());
    }

    /** État de l'ordre (l'application interroge après le retour du navigateur). */
    @GetMapping("/api/portefeuille/paiement/statut/{orderId}")
    public StatutPaiementResponse statut(@PathVariable String orderId) {
        return passerelleService.statut(orderId);
    }

    // -------------------------------------------------------- pages publiques

    /** Page de paiement de marque (ouverte dans le navigateur). */
    @GetMapping(value = "/pay/{orderId}", produces = MediaType.TEXT_HTML_VALUE)
    public String pagePaiement(@PathVariable String orderId) {
        PaiementSimule o = passerelleService.get(orderId);
        if ("PAYE".equals(o.getStatut())) {
            return pageSucces(o);
        }
        return pageFormulaire(o);
    }

    /** Confirmation du paiement (soumission du formulaire de la passerelle). */
    @PostMapping(value = "/pay/{orderId}/confirmer", produces = MediaType.TEXT_HTML_VALUE)
    public String confirmer(@PathVariable String orderId) {
        PaiementSimule o = passerelleService.confirmer(orderId);
        return pageSucces(o);
    }

    // ---------------------------------------------------------------- rendu

    private record Marque(String nom, String couleur, String couleur2, String emoji) {
    }

    private Marque marque(String moyen) {
        return switch (moyen) {
            case "MONCASH" -> new Marque("MonCash", "#C8102E", "#8B0000", "📱");
            case "NATCASH" -> new Marque("NatCash", "#009639", "#00662A", "📱");
            case "VISA" -> new Marque("Visa", "#1A1F71", "#0E1247", "💳");
            case "MASTERCARD" -> new Marque("Mastercard", "#EB001B", "#B3000F", "💳");
            case "VIREMENT" -> new Marque("Virement bancaire", "#2563EB", "#1E3A8A", "🏦");
            default -> new Marque("Paiement", "#3A22A8", "#140A38", "💰");
        };
    }

    /** Champs du formulaire adaptés au moyen (téléphone/PIN, carte, ou banque). */
    private String champs(String moyen) {
        boolean portefeuille = moyen.equals("MONCASH") || moyen.equals("NATCASH");
        boolean carte = moyen.equals("VISA") || moyen.equals("MASTERCARD");
        if (portefeuille) {
            return """
                    <label>Numéro de téléphone</label>
                    <input type="tel" placeholder="+509 3X XX XX XX" required>
                    <label>Code PIN</label>
                    <input type="password" placeholder="••••" maxlength="6" required>
                    """;
        }
        if (carte) {
            return """
                    <label>Numéro de carte</label>
                    <input type="text" placeholder="4242 4242 4242 4242" maxlength="19" required>
                    <div class="row">
                      <div><label>Expiration</label>
                        <input type="text" placeholder="MM/AA" maxlength="5" required></div>
                      <div><label>CVV</label>
                        <input type="password" placeholder="•••" maxlength="4" required></div>
                    </div>
                    """;
        }
        return """
                <label>Nom du titulaire</label>
                <input type="text" placeholder="Nom complet" required>
                <label>Numéro de compte</label>
                <input type="text" placeholder="Compte bancaire" required>
                """;
    }

    private String pageFormulaire(PaiementSimule o) {
        Marque mq = marque(o.getMoyen());
        String action = "/pay/" + echapper(o.getId()) + "/confirmer";
        String titre = ("DEPOT".equals(o.getSens()) ? "Dépôt" : "Retrait") + " · SOL EN LIGNE";
        String bouton = "DEPOT".equals(o.getSens()) ? "Payer" : "Confirmer le retrait";

        return """
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                  *{box-sizing:border-box;font-family:-apple-system,Segoe UI,Roboto,sans-serif}
                  body{margin:0;background:#EEF0F5;min-height:100vh;display:flex;
                       align-items:center;justify-content:center;padding:18px}
                  .carte{background:#fff;border-radius:22px;max-width:400px;width:100%%;
                         overflow:hidden;box-shadow:0 18px 50px rgba(0,0,0,.22)}
                  .tete{background:linear-gradient(135deg,%s,%s);color:#fff;padding:26px 24px}
                  .tete .logo{font-size:26px;font-weight:800;letter-spacing:.5px}
                  .tete .em{font-size:34px}
                  .montant{font-size:30px;font-weight:800;margin-top:12px}
                  .sous{opacity:.85;font-size:13px;margin-top:2px}
                  form{padding:22px 24px 8px}
                  label{display:block;font-size:12px;color:#6B7280;margin:12px 0 5px;font-weight:600}
                  input{width:100%%;padding:12px 14px;border:1px solid #D8DBE3;border-radius:12px;
                        font-size:15px;outline:none}
                  input:focus{border-color:%s}
                  .row{display:flex;gap:12px}.row>div{flex:1}
                  .btn{width:100%%;margin-top:20px;padding:15px;border:none;border-radius:14px;
                       background:%s;color:#fff;font-size:16px;font-weight:700;cursor:pointer}
                  .ref{text-align:center;color:#9CA3AF;font-size:12px;padding:14px 24px 6px}
                  .demo{text-align:center;background:#FFF7E6;color:#92660A;font-size:11.5px;
                        padding:8px;border-top:1px solid #F3E4Bf}
                </style></head>
                <body><div class="carte">
                  <div class="tete">
                    <div class="em">%s</div>
                    <div class="logo">%s</div>
                    <div class="montant">%s HTG</div>
                    <div class="sous">%s vers votre portefeuille SOL EN LIGNE</div>
                  </div>
                  <form method="post" action="%s">
                    %s
                    <button class="btn" type="submit">%s</button>
                  </form>
                  <div class="ref">Référence : %s</div>
                  <div class="demo">🔒 Environnement de démonstration — aucun argent réel n'est débité.</div>
                </div></body></html>
                """.formatted(
                echapper(titre), mq.couleur(), mq.couleur2(), mq.couleur(), mq.couleur(),
                mq.emoji(), echapper(mq.nom()), formater(o.getMontant()),
                "DEPOT".equals(o.getSens()) ? "Rechargement" : "Retrait",
                action, champs(o.getMoyen()), bouton, echapper(o.getReference()));
    }

    private String pageSucces(PaiementSimule o) {
        Marque mq = marque(o.getMoyen());
        String libelle = "DEPOT".equals(o.getSens()) ? "Dépôt effectué" : "Retrait effectué";
        return """
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Paiement réussi — SOL EN LIGNE</title>
                <style>
                  *{box-sizing:border-box;font-family:-apple-system,Segoe UI,Roboto,sans-serif}
                  body{margin:0;background:linear-gradient(160deg,%s,%s);min-height:100vh;
                       display:flex;align-items:center;justify-content:center;padding:20px}
                  .carte{background:#fff;border-radius:24px;max-width:400px;width:100%%;
                         padding:34px 28px;text-align:center;box-shadow:0 20px 60px rgba(0,0,0,.3)}
                  .rond{width:84px;height:84px;border-radius:50%%;background:#1B8A4E22;margin:0 auto 16px;
                        display:flex;align-items:center;justify-content:center;font-size:46px}
                  h1{color:#1B8A4E;font-size:23px;margin:0 0 6px}
                  .montant{font-size:30px;font-weight:800;color:#111;margin:10px 0}
                  .info{color:#6B7280;font-size:14px;line-height:1.5}
                  .ref{color:#9CA3AF;font-size:12px;margin-top:16px}
                  .retour{margin-top:22px;background:#F1F1F7;border-radius:12px;padding:13px;
                          color:#3A22A8;font-weight:700;font-size:14px}
                </style></head>
                <body><div class="carte">
                  <div class="rond">✅</div>
                  <h1>%s</h1>
                  <div class="montant">%s HTG</div>
                  <div class="info">Via %s · Opération scellée dans le Registre Inviolable.</div>
                  <div class="ref">Référence : %s</div>
                  <div class="retour">↩︎ Retournez dans l'application SOL EN LIGNE</div>
                </div></body></html>
                """.formatted(mq.couleur(), mq.couleur2(), echapper(libelle),
                formater(o.getMontant()), echapper(mq.nom()), echapper(o.getReference()));
    }

    private String formater(BigDecimal montant) {
        return new DecimalFormat("#,##0.00").format(montant);
    }

    private String echapper(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
