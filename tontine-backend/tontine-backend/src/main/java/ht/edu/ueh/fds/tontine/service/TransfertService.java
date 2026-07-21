package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.AnnuaireDtos.RechercheUtilisateurResponse;
import ht.edu.ueh.fds.tontine.dto.TransfertDtos.*;
import ht.edu.ueh.fds.tontine.entity.Transfert;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.TransfertRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transfert d'argent entre utilisateurs : envoi avec reçu (numéro de
 * confirmation + identifiant de transaction), historique filtrable, détail, et
 * vérification publique. Chaque transfert débite/crédite les portefeuilles (ce
 * qui scelle des blocs au Registre Inviolable) et est enregistré comme reçu.
 */
@Service
public class TransfertService {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final SecureRandom aleatoire = new SecureRandom();

    private final AnnuaireService annuaireService;
    private final PortefeuilleService portefeuilleService;
    private final UtilisateurRepository utilisateurRepository;
    private final TransfertRepository transfertRepository;
    private final KycService kycService;
    private final String urlPublique;

    public TransfertService(AnnuaireService annuaireService,
                            PortefeuilleService portefeuilleService,
                            UtilisateurRepository utilisateurRepository,
                            TransfertRepository transfertRepository,
                            KycService kycService,
                            @Value("${app.public-url:http://localhost:8080}") String urlPublique) {
        this.annuaireService = annuaireService;
        this.portefeuilleService = portefeuilleService;
        this.utilisateurRepository = utilisateurRepository;
        this.transfertRepository = transfertRepository;
        this.kycService = kycService;
        this.urlPublique = urlPublique;
    }

    /** Effectue le transfert et renvoie le reçu numérique. */
    @Transactional
    public RecuTransfertResponse transferer(String expediteurId, TransfertRequest req) {
        // Règle stricte : aucun transfert tant que l'identité n'est pas vérifiée.
        kycService.exigerVerifie(expediteurId);

        BigDecimal montant = req.montant();
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du transfert doit être positif.");
        }
        // Résout le bénéficiaire (exclut déjà soi-même et les comptes indisponibles).
        RechercheUtilisateurResponse benef = annuaireService.rechercher(req.beneficiaire(), expediteurId);
        Utilisateur exp = utilisateurRepository.findById(expediteurId)
                .orElseThrow(() -> new BusinessException("Expéditeur introuvable."));

        String devise = (req.devise() == null || req.devise().isBlank()) ? "HTG" : req.devise().trim();
        String note = (req.message() == null || req.message().isBlank()) ? null : req.message().trim();
        String suffixe = note == null ? "" : " — " + note;

        // Débit expéditeur (vérifie le solde) puis crédit bénéficiaire ; les deux
        // scellent un bloc au registre. La transaction assure l'atomicité.
        portefeuilleService.debiter(expediteurId, montant, "TRANSFERT",
                "Transfert à @" + benef.username() + suffixe);
        portefeuilleService.crediter(benef.id(), montant, "TRANSFERT",
                "Reçu de @" + exp.getUsername() + suffixe);

        BigDecimal soldeRestant = portefeuilleService.solde(expediteurId);
        String reference = genererReference();
        Transfert t = transfertRepository.save(Transfert.builder()
                .id(UUID.randomUUID().toString())
                .reference(reference)
                .transactionId(genererTransactionId())
                .expediteurId(expediteurId)
                .expediteurUsername(exp.getUsername())
                .expediteurNom(exp.getPrenom() + " " + exp.getNom())
                .beneficiaireId(benef.id())
                .beneficiaireUsername(benef.username())
                .beneficiaireNom(benef.nomComplet())
                .montant(montant)
                .devise(devise)
                .frais(BigDecimal.ZERO)
                .message(note)
                .statut("REUSSI")
                .methodeAuth(req.methodeAuth())
                .soldeApresExpediteur(soldeRestant)
                .dateCreation(LocalDateTime.now())
                .build());

        return new RecuTransfertResponse(
                t.getId(), t.getReference(), t.getTransactionId(), t.getStatut(),
                t.getMontant(), t.getDevise(), t.getFrais(), t.getMontant().add(t.getFrais()),
                t.getMessage(), t.getDateCreation(),
                t.getExpediteurNom(), t.getExpediteurUsername(),
                t.getBeneficiaireNom(), t.getBeneficiaireUsername(),
                soldeRestant,
                urlPublique.replaceAll("/+$", "") + "/api/transferts/verifier/" + reference);
    }

    /** Historique (envoyés + reçus) filtré et cherché. */
    @Transactional(readOnly = true)
    public List<TransfertHistoriqueItem> historique(String userId, String filtre, String recherche) {
        String f = filtre == null ? "TOUS" : filtre.trim().toUpperCase();
        String q = recherche == null ? "" : recherche.trim().toLowerCase();
        LocalDateTime maintenant = LocalDateTime.now();

        List<TransfertHistoriqueItem> items = new ArrayList<>();
        for (Transfert t : transfertRepository.historique(userId)) {
            boolean envoye = t.getExpediteurId().equals(userId);
            String sens = envoye ? "ENVOYE" : "RECU";

            // Filtre par sens / statut / période.
            if (f.equals("ENVOYES") && !envoye) continue;
            if (f.equals("RECUS") && envoye) continue;
            if ((f.equals("REUSSI") || f.equals("EN_ATTENTE") || f.equals("ECHEC") || f.equals("ANNULE"))
                    && !f.equals(t.getStatut())) continue;
            if (f.equals("AUJOURDHUI")
                    && !t.getDateCreation().toLocalDate().isEqual(maintenant.toLocalDate())) continue;
            if (f.equals("SEMAINE")
                    && t.getDateCreation().isBefore(maintenant.minus(7, ChronoUnit.DAYS))) continue;
            if (f.equals("MOIS")
                    && t.getDateCreation().isBefore(maintenant.minus(30, ChronoUnit.DAYS))) continue;

            String autreNom = envoye ? t.getBeneficiaireNom() : t.getExpediteurNom();
            String autreUsername = envoye ? t.getBeneficiaireUsername() : t.getExpediteurUsername();

            // Recherche texte (nom, username, référence, id transaction).
            if (!q.isEmpty()) {
                String cible = (nn(autreNom) + " " + nn(autreUsername) + " "
                        + nn(t.getReference()) + " " + nn(t.getTransactionId())).toLowerCase();
                if (!cible.contains(q)) continue;
            }

            String autrePhoto = utilisateurRepository
                    .findById(envoye ? t.getBeneficiaireId() : t.getExpediteurId())
                    .map(Utilisateur::getPhotoUrl).orElse(null);

            items.add(new TransfertHistoriqueItem(
                    t.getId(), sens, autreNom, autreUsername, autrePhoto,
                    t.getMontant(), t.getDevise(), t.getStatut(), t.getDateCreation(),
                    t.getReference(), t.getTransactionId()));
        }
        return items;
    }

    /** Détail d'un transfert (accessible à l'expéditeur ou au bénéficiaire). */
    @Transactional(readOnly = true)
    public TransfertDetailResponse detail(String userId, String transfertId) {
        Transfert t = transfertRepository.findById(transfertId)
                .orElseThrow(() -> new BusinessException("Transfert introuvable."));
        if (!t.getExpediteurId().equals(userId) && !t.getBeneficiaireId().equals(userId)) {
            throw new BusinessException("Accès refusé à ce transfert.");
        }
        String sens = t.getExpediteurId().equals(userId) ? "ENVOYE" : "RECU";
        return new TransfertDetailResponse(
                t.getId(), sens, t.getStatut(), t.getMontant(), t.getDevise(), t.getFrais(),
                t.getExpediteurNom(), t.getExpediteurUsername(),
                t.getBeneficiaireNom(), t.getBeneficiaireUsername(),
                t.getDateCreation(), t.getReference(), t.getTransactionId(),
                t.getMessage(), t.getMethodeAuth());
    }

    /** Vérification publique d'un reçu par son numéro de confirmation (QR). */
    @Transactional(readOnly = true)
    public Transfert verifier(String reference) {
        return transfertRepository.findByReference(reference).orElse(null);
    }

    // ------------------------------------------------------------- références

    private String genererReference() {
        String ref;
        do {
            int n = 100000 + aleatoire.nextInt(900000);
            ref = "SOL-" + LocalDate.now().format(JOUR) + "-" + n;
        } while (transfertRepository.existsByReference(ref));
        return ref;
    }

    private String genererTransactionId() {
        StringBuilder sb = new StringBuilder("TX-");
        for (int i = 0; i < 10; i++) {
            sb.append("0123456789ABCDEF".charAt(aleatoire.nextInt(16)));
        }
        return sb.toString();
    }

    private String nn(String s) {
        return s == null ? "" : s;
    }
}
