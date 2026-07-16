package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.ReleveResponse;
import ht.edu.ueh.fds.tontine.dto.VerificationReleveResponse;
import ht.edu.ueh.fds.tontine.entity.CertificatFiabilite;
import ht.edu.ueh.fds.tontine.entity.Cotisation;
import ht.edu.ueh.fds.tontine.entity.MembreSol;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.CertificatFiabiliteRepository;
import ht.edu.ueh.fds.tontine.repository.CotisationRepository;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * Genere et verifie le Certificat de Fiabilite Financiere d'un membre.
 * Le score est calcule cote serveur (autoritatif, donc non falsifiable) a
 * partir de l'historique reel des cotisations. Chaque certificat porte une
 * reference unique et une empreinte SHA-256 : une banque peut ainsi verifier
 * son authenticite via l'endpoint public de verification.
 */
@Service
public class ReleveService {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom aleatoire = new SecureRandom();

    private final MembreSolRepository membreSolRepository;
    private final CotisationRepository cotisationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CertificatFiabiliteRepository certificatRepository;
    private final String secret;
    private final String urlPublique;

    public ReleveService(
            MembreSolRepository membreSolRepository,
            CotisationRepository cotisationRepository,
            UtilisateurRepository utilisateurRepository,
            CertificatFiabiliteRepository certificatRepository,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.public-url:http://localhost:8080}") String urlPublique) {
        this.membreSolRepository = membreSolRepository;
        this.cotisationRepository = cotisationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.certificatRepository = certificatRepository;
        this.secret = secret;
        this.urlPublique = urlPublique;
    }

    /**
     * Genere un nouveau Certificat de Fiabilite pour l'utilisateur connecte,
     * l'enregistre (pour une verification ulterieure), et le renvoie.
     */
    @Transactional
    public ReleveResponse genererReleve(String utilisateurId) {
        Utilisateur u = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable."));

        var participations = membreSolRepository.findByUtilisateurId(utilisateurId).stream()
                .filter(m -> !"REFUSE".equals(m.getStatutMembre()))
                .toList();
        int nbSols = participations.size();

        LocalDate aujourdHui = LocalDate.now();
        BigDecimal totalCotise = BigDecimal.ZERO;
        int aTemps = 0, retards = 0, defauts = 0;

        for (MembreSol m : participations) {
            for (Cotisation c : cotisationRepository
                    .findByMembreSolIdOrderByDateEcheanceDesc(m.getId())) {
                boolean payee = "VALIDE".equals(c.getStatut());
                boolean echue = c.getDateEcheance() != null
                        && c.getDateEcheance().isBefore(aujourdHui);
                if (payee) {
                    totalCotise = totalCotise.add(
                            c.getMontantPaye() == null ? BigDecimal.ZERO : c.getMontantPaye());
                    boolean aLHeure = c.getDatePaiementEffectif() == null
                            || c.getDateEcheance() == null
                            || !c.getDatePaiementEffectif().toLocalDate().isAfter(c.getDateEcheance());
                    if (aLHeure) aTemps++; else retards++;
                } else if (echue) {
                    defauts++;
                }
            }
        }

        int evaluees = aTemps + retards + defauts;
        boolean historiqueSuffisant = evaluees > 0;
        int score = historiqueSuffisant ? (aTemps * 100) / evaluees : 100;
        String note = note(score, historiqueSuffisant);
        String niveau = niveau(note);

        // Reference unique + empreinte anti-falsification.
        String reference = genererReferenceUnique();
        String hash = calculerHash(reference, utilisateurId, score, note, totalCotise);

        CertificatFiabilite certificat = certificatRepository.save(CertificatFiabilite.builder()
                .reference(reference)
                .utilisateurId(utilisateurId)
                .nomComplet(u.getPrenom() + " " + u.getNom())
                .membreDepuis(u.getDateCreation() == null ? null : u.getDateCreation().toLocalDate())
                .nbSols(nbSols)
                .totalCotise(totalCotise)
                .nbCotisations(evaluees)
                .nbATemps(aTemps)
                .nbRetards(retards)
                .nbDefauts(defauts)
                .scoreGlobal(score)
                .note(note)
                .hash(hash)
                .build());

        return new ReleveResponse(
                certificat.getReference(),
                certificat.getNomComplet(),
                certificat.getMembreDepuis(),
                certificat.getNbSols(),
                certificat.getTotalCotise(),
                certificat.getNbCotisations(),
                certificat.getNbATemps(),
                certificat.getNbRetards(),
                certificat.getNbDefauts(),
                certificat.getScoreGlobal(),
                certificat.getNote(),
                niveau,
                historiqueSuffisant,
                certificat.getDateEmission(),
                certificat.getHash(),
                urlPublique.replaceAll("/+$", "") + "/api/releve/verifier/" + reference);
    }

    /** Verifie un relevé par sa reference (endpoint public consulte par un tiers). */
    @Transactional(readOnly = true)
    public VerificationReleveResponse verifier(String reference) {
        return certificatRepository.findByReference(reference)
                .map(c -> new VerificationReleveResponse(
                        true,
                        c.getNomComplet(),
                        c.getScoreGlobal(),
                        c.getNote(),
                        niveau(c.getNote()),
                        c.getDateEmission(),
                        "Relevé authentique, émis par SOL EN LIGNE."))
                .orElse(new VerificationReleveResponse(
                        false, null, 0, "-", "-", null,
                        "Aucun relevé ne correspond à cette référence. Document non authentique."));
    }

    // ------------------------------------------------------------------ helpers

    private String note(int score, boolean historiqueSuffisant) {
        if (!historiqueSuffisant) return "N";
        if (score >= 90) return "A";
        if (score >= 75) return "B";
        if (score >= 50) return "C";
        return "D";
    }

    private String niveau(String note) {
        return switch (note) {
            case "A" -> "Excellent — membre exemplaire";
            case "B" -> "Bon — membre fiable";
            case "C" -> "Moyen — en progression";
            case "D" -> "À surveiller";
            default -> "Nouveau membre — historique insuffisant";
        };
    }

    private String genererReferenceUnique() {
        String reference;
        do {
            StringBuilder sb = new StringBuilder("SOL-");
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHABET[aleatoire.nextInt(ALPHABET.length)]);
            }
            reference = sb.toString();
        } while (certificatRepository.existsByReference(reference));
        return reference;
    }

    /** Empreinte SHA-256 du contenu essentiel + secret serveur (anti-falsification). */
    private String calculerHash(String reference, String userId, int score,
                                String note, BigDecimal total) {
        String contenu = String.join("|",
                reference, userId, String.valueOf(score), note,
                total.toPlainString(), secret);
        try {
            byte[] octets = MessageDigest.getInstance("SHA-256")
                    .digest(contenu.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(octets.length * 2);
            for (byte b : octets) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException("Erreur lors de la génération du certificat.");
        }
    }
}
