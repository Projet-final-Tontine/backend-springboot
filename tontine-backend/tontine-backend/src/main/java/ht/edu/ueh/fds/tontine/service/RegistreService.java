package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.BlocRegistreResponse;
import ht.edu.ueh.fds.tontine.dto.VerificationRegistreResponse;
import ht.edu.ueh.fds.tontine.entity.BlocRegistre;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.BlocRegistreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Registre Inviolable — le grand livre de comptes infalsifiable de la plateforme.
 *
 * Chaque mouvement d'argent est scellé dans un {@link BlocRegistre} chaîné au
 * précédent par son empreinte. Le hash d'un bloc dépend de son contenu, du hash
 * du bloc précédent ET d'un secret serveur : personne ne peut modifier une
 * écriture passée sans invalider toute la suite de la chaîne, ni recalculer une
 * empreinte valide sans le secret.
 *
 * <p>Le scellement est {@code synchronized} : les blocs sont ajoutés un par un,
 * dans l'ordre, ce qui garantit un chaînage cohérent.</p>
 */
@Service
public class RegistreService {

    /** Hash conventionnel du « bloc zéro » (genèse) : 64 zéros. */
    private static final String HASH_GENESE = "0".repeat(64);

    private final BlocRegistreRepository registreRepository;
    private final String secret;

    public RegistreService(BlocRegistreRepository registreRepository,
                           @Value("${app.jwt.secret}") String secret) {
        this.registreRepository = registreRepository;
        this.secret = secret;
    }

    /**
     * Scelle un nouveau mouvement dans le registre. Appelé automatiquement à
     * chaque écriture du portefeuille ; ne doit jamais faire échouer la
     * transaction financière (le registre est une garantie, pas un obstacle).
     */
    @Transactional
    public synchronized void sceller(String type, String sens, BigDecimal montant,
                                     String utilisateurId, String description,
                                     String transactionId) {
        BlocRegistre precedent = registreRepository.findTopByOrderByPositionDesc().orElse(null);
        long position = (precedent == null) ? 0L : precedent.getPosition() + 1;
        String hashPrecedent = (precedent == null) ? HASH_GENESE : precedent.getHash();

        LocalDateTime date = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        BigDecimal montantNorme = normaliser(montant);

        String hash = calculerHash(position, date, type, sens, montantNorme,
                utilisateurId, description, hashPrecedent);

        registreRepository.save(BlocRegistre.builder()
                .id(UUID.randomUUID().toString())
                .position(position)
                .dateScellement(date)
                .type(type)
                .sens(sens)
                .montant(montantNorme)
                .utilisateurId(utilisateurId)
                .description(description)
                .transactionId(transactionId)
                .hashPrecedent(hashPrecedent)
                .hash(hash)
                .build());
    }

    /**
     * Vérifie l'intégrité de toute la chaîne : recalcule chaque empreinte et
     * contrôle le chaînage. Renvoie la position du premier bloc corrompu, le cas
     * échéant. C'est cette méthode qui « attrape » toute falsification en base.
     */
    @Transactional(readOnly = true)
    public VerificationRegistreResponse verifier() {
        List<BlocRegistre> chaine = registreRepository.findAllByOrderByPositionAsc();
        if (chaine.isEmpty()) {
            return new VerificationRegistreResponse(true, 0, null, HASH_GENESE,
                    "Registre vide : aucune transaction scellée pour l'instant.");
        }

        String hashAttendu = HASH_GENESE;
        long positionAttendue = 0;
        for (BlocRegistre bloc : chaine) {
            // 1) le chaînage : ce bloc doit pointer vers le hash du précédent
            if (!hashAttendu.equals(bloc.getHashPrecedent())
                    || bloc.getPosition() != positionAttendue) {
                return rupture(bloc.getPosition(), chaine.size());
            }
            // 2) l'intégrité : le hash recalculé doit correspondre au hash stocké
            String recalcule = calculerHash(bloc.getPosition(), bloc.getDateScellement(),
                    bloc.getType(), bloc.getSens(), normaliser(bloc.getMontant()),
                    bloc.getUtilisateurId(), bloc.getDescription(), bloc.getHashPrecedent());
            if (!recalcule.equals(bloc.getHash())) {
                return rupture(bloc.getPosition(), chaine.size());
            }
            hashAttendu = bloc.getHash();
            positionAttendue++;
        }

        String empreinte = chaine.get(chaine.size() - 1).getHash();
        return new VerificationRegistreResponse(true, chaine.size(), null, empreinte,
                "Chaîne intègre : les " + chaine.size()
                        + " écritures sont authentiques et non modifiées.");
    }

    private VerificationRegistreResponse rupture(long position, int total) {
        return new VerificationRegistreResponse(false, total, position, null,
                "⚠️ Falsification détectée au bloc #" + position
                        + " : le registre a été altéré et n'est plus fiable.");
    }

    /** Blocs du plus récent au plus ancien (affichage). */
    @Transactional(readOnly = true)
    public List<BlocRegistreResponse> lister() {
        return registreRepository.findAllByOrderByPositionDesc()
                .stream().map(BlocRegistreResponse::from).toList();
    }

    // ------------------------------------------------------------------ hash

    /** Normalise un montant à 2 décimales pour un hachage stable après relecture. */
    private BigDecimal normaliser(BigDecimal montant) {
        return (montant == null ? BigDecimal.ZERO : montant).setScale(2, RoundingMode.HALF_UP);
    }

    /** Empreinte SHA-256 du contenu du bloc + hash précédent + secret serveur. */
    private String calculerHash(long position, LocalDateTime date, String type, String sens,
                                BigDecimal montant, String utilisateurId, String description,
                                String hashPrecedent) {
        String contenu = String.join("|",
                String.valueOf(position),
                date.toString(),
                nn(type), nn(sens),
                montant.toPlainString(),
                nn(utilisateurId), nn(description),
                hashPrecedent, secret);
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
            throw new BusinessException("Erreur lors du scellement du registre.");
        }
    }

    private String nn(String s) {
        return s == null ? "" : s;
    }
}
