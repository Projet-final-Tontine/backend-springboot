package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.PortefeuilleResponse;
import ht.edu.ueh.fds.tontine.dto.TransactionPortefeuilleResponse;
import ht.edu.ueh.fds.tontine.entity.Portefeuille;
import ht.edu.ueh.fds.tontine.entity.TransactionPortefeuille;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.PortefeuilleRepository;
import ht.edu.ueh.fds.tontine.repository.TransactionPortefeuilleRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Regles metier du portefeuille (wallet) centralise.
 *
 * Toute la monnaie transite par la plateforme :
 * - les membres alimentent leur solde par des depots (Mon Cash) ;
 * - les cotisations sont prelevees uniquement sur ce solde ;
 * - le gain de la « main » est credite au portefeuille du beneficiaire.
 *
 * Chaque mouvement est enregistre dans le journal ({@link TransactionPortefeuille})
 * pour une tracabilite complete.
 */
@Service
@RequiredArgsConstructor
public class PortefeuilleService {

    private final PortefeuilleRepository portefeuilleRepository;
    private final TransactionPortefeuilleRepository transactionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RegistreService registreService;

    /** Recupere le portefeuille de l'utilisateur, en le creant au premier acces. */
    @Transactional
    public Portefeuille getOuCreer(String utilisateurId) {
        return portefeuilleRepository.findByUtilisateurId(utilisateurId)
                .orElseGet(() -> {
                    Utilisateur u = utilisateurRepository.findById(utilisateurId)
                            .orElseThrow(() -> new BusinessException(
                                    "Utilisateur introuvable : " + utilisateurId));
                    return portefeuilleRepository.save(Portefeuille.builder()
                            .utilisateur(u)
                            .solde(BigDecimal.ZERO)
                            .build());
                });
    }

    /** Solde disponible de l'utilisateur. */
    @Transactional
    public BigDecimal solde(String utilisateurId) {
        return getOuCreer(utilisateurId).getSolde();
    }

    /**
     * Cas « Deposer de l'argent » : credite le portefeuille.
     * La confirmation Mon Cash est simulee pour l'instant ; l'integration de
     * l'API marchande remplacera cette etape par une verification automatique.
     */
    @Transactional
    public Portefeuille deposer(String utilisateurId, BigDecimal montant, String referenceMonCash) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du depot doit etre positif.");
        }
        Portefeuille p = getOuCreer(utilisateurId);
        BigDecimal nouveauSolde = p.getSolde().add(montant);
        p.setSolde(nouveauSolde);
        portefeuilleRepository.save(p);

        enregistrer(p, "DEPOT", "CREDIT", montant, nouveauSolde,
                referenceMonCash, "Depot Mon Cash");
        return p;
    }

    /**
     * Debite le portefeuille pour regler une cotisation.
     * Rejette l'operation si le solde est insuffisant : l'appelant doit alors
     * inviter le membre a deposer de l'argent.
     */
    @Transactional
    public Portefeuille debiterCotisation(String utilisateurId, BigDecimal montant, String description) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant a debiter doit etre positif.");
        }
        Portefeuille p = getOuCreer(utilisateurId);
        if (p.getSolde().compareTo(montant) < 0) {
            throw new BusinessException(
                    "Solde insuffisant. Veuillez deposer de l'argent avant de payer votre cotisation.");
        }
        BigDecimal nouveauSolde = p.getSolde().subtract(montant);
        p.setSolde(nouveauSolde);
        portefeuilleRepository.save(p);

        enregistrer(p, "COTISATION", "DEBIT", montant, nouveauSolde, null, description);
        return p;
    }

    /** Debit generique du portefeuille (ex : contribution au Fon Sekou). */
    @Transactional
    public Portefeuille debiter(String utilisateurId, BigDecimal montant, String type, String description) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant a debiter doit etre positif.");
        }
        Portefeuille p = getOuCreer(utilisateurId);
        if (p.getSolde().compareTo(montant) < 0) {
            throw new BusinessException("Solde insuffisant. Veuillez deposer de l'argent d'abord.");
        }
        BigDecimal nouveauSolde = p.getSolde().subtract(montant);
        p.setSolde(nouveauSolde);
        portefeuilleRepository.save(p);
        enregistrer(p, type, "DEBIT", montant, nouveauSolde, null, description);
        return p;
    }

    /** Credit generique du portefeuille (ex : secours du Fon Sekou). */
    @Transactional
    public Portefeuille crediter(String utilisateurId, BigDecimal montant, String type, String description) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant a crediter doit etre positif.");
        }
        Portefeuille p = getOuCreer(utilisateurId);
        BigDecimal nouveauSolde = p.getSolde().add(montant);
        p.setSolde(nouveauSolde);
        portefeuilleRepository.save(p);
        enregistrer(p, type, "CREDIT", montant, nouveauSolde, null, description);
        return p;
    }

    /** Credite le portefeuille du beneficiaire lors du versement de la « main ». */
    @Transactional
    public Portefeuille crediterGain(String utilisateurId, BigDecimal montant, String description) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant a crediter doit etre positif.");
        }
        Portefeuille p = getOuCreer(utilisateurId);
        BigDecimal nouveauSolde = p.getSolde().add(montant);
        p.setSolde(nouveauSolde);
        portefeuilleRepository.save(p);

        enregistrer(p, "GAIN_MAIN", "CREDIT", montant, nouveauSolde, null, description);
        return p;
    }

    /**
     * Depot confirme par la passerelle de paiement (Mon Cash, NatCash, carte...).
     * Credite le portefeuille en tracant le moyen utilise ; scelle un bloc au
     * Registre Inviolable via {@link #enregistrer}.
     */
    @Transactional
    public Portefeuille deposerParMoyen(String utilisateurId, BigDecimal montant,
                                        String moyen, String reference) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du depot doit etre positif.");
        }
        Portefeuille p = getOuCreer(utilisateurId);
        BigDecimal nouveauSolde = p.getSolde().add(montant);
        p.setSolde(nouveauSolde);
        portefeuilleRepository.save(p);
        enregistrer(p, "DEPOT", "CREDIT", montant, nouveauSolde, reference, "Depot " + moyen);
        return p;
    }

    /**
     * Retrait confirme par la passerelle. Debite le portefeuille (rejet si solde
     * insuffisant) et scelle un bloc au Registre Inviolable.
     */
    @Transactional
    public Portefeuille retirerParMoyen(String utilisateurId, BigDecimal montant,
                                        String moyen, String reference) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Le montant du retrait doit etre positif.");
        }
        Portefeuille p = getOuCreer(utilisateurId);
        if (p.getSolde().compareTo(montant) < 0) {
            throw new BusinessException("Solde insuffisant pour ce retrait.");
        }
        BigDecimal nouveauSolde = p.getSolde().subtract(montant);
        p.setSolde(nouveauSolde);
        portefeuilleRepository.save(p);
        enregistrer(p, "RETRAIT", "DEBIT", montant, nouveauSolde, reference, "Retrait " + moyen);
        return p;
    }

    /** Journal des mouvements du portefeuille (du plus recent au plus ancien). */
    @Transactional
    public List<TransactionPortefeuille> historique(String utilisateurId) {
        Portefeuille p = getOuCreer(utilisateurId);
        return transactionRepository.findByPortefeuilleIdOrderByDateCreationDesc(p.getId());
    }

    /**
     * Vue complete du portefeuille (solde + journal), construite dans la
     * transaction pour eviter tout probleme de chargement paresseux.
     */
    @Transactional
    public PortefeuilleResponse consulter(String utilisateurId) {
        Portefeuille p = getOuCreer(utilisateurId);
        List<TransactionPortefeuilleResponse> mouvements =
                transactionRepository.findByPortefeuilleIdOrderByDateCreationDesc(p.getId())
                        .stream().map(TransactionPortefeuilleResponse::from).toList();
        return new PortefeuilleResponse(p.getId(), p.getSolde(), mouvements);
    }

    private void enregistrer(Portefeuille p, String type, String sens, BigDecimal montant,
                             BigDecimal soldeApres, String reference, String description) {
        TransactionPortefeuille tx = transactionRepository.save(TransactionPortefeuille.builder()
                .portefeuille(p)
                .type(type)
                .sens(sens)
                .montant(montant)
                .soldeApres(soldeApres)
                .referenceExterne(reference)
                .description(description)
                .build());

        // Registre Inviolable : chaque mouvement d'argent est scellé dans le
        // grand livre à hash chaîné (aucun gourde ne bouge sans trace scellée).
        registreService.sceller(type, sens, montant,
                p.getUtilisateur() != null ? p.getUtilisateur().getId() : null,
                description, tx.getId());
    }
}
