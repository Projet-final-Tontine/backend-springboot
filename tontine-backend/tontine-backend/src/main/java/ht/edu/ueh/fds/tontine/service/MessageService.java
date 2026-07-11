package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.MessageResponse;
import ht.edu.ueh.fds.tontine.entity.Message;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.MembreSolRepository;
import ht.edu.ueh.fds.tontine.repository.MessageRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regles metier de la messagerie : chat de groupe d'un Sol et chat prive.
 * Les reponses sont construites dans la transaction pour charger l'expediteur.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int LONGUEUR_MAX = 2000;

    private final MessageRepository messageRepository;
    private final MembreSolRepository membreSolRepository;
    private final UtilisateurRepository utilisateurRepository;

    // ---------- Chat de groupe (Sol) ----------

    /** Envoyer un message dans le chat de groupe d'un Sol (reserve aux membres). */
    @Transactional
    public MessageResponse envoyerAuSol(String expediteurId, String solId,
                                        String contenu, String pieceJointeUrl, String typePiece) {
        String texte = preparer(contenu, pieceJointeUrl);
        if (!membreSolRepository.existsByUtilisateurIdAndSolId(expediteurId, solId)) {
            throw new BusinessException("Vous n'etes pas membre de ce Sol.");
        }
        Utilisateur expediteur = exiger(expediteurId);
        Message message = messageRepository.save(Message.builder()
                .expediteur(expediteur)
                .solId(solId)
                .contenu(texte)
                .pieceJointeUrl(vide(pieceJointeUrl))
                .typePiece(vide(typePiece))
                .build());
        return MessageResponse.from(message);
    }

    /** Messages du chat de groupe d'un Sol. */
    @Transactional(readOnly = true)
    public List<MessageResponse> messagesDuSol(String utilisateurId, String solId) {
        if (!membreSolRepository.existsByUtilisateurIdAndSolId(utilisateurId, solId)) {
            throw new BusinessException("Vous n'etes pas membre de ce Sol.");
        }
        return messageRepository.findBySolIdOrderByDateEnvoiAsc(solId).stream()
                .map(MessageResponse::from).toList();
    }

    // ---------- Chat prive ----------

    /** Envoyer un message prive a un autre utilisateur. */
    @Transactional
    public MessageResponse envoyerPrive(String expediteurId, String destinataireId,
                                        String contenu, String pieceJointeUrl, String typePiece) {
        String texte = preparer(contenu, pieceJointeUrl);
        if (expediteurId.equals(destinataireId)) {
            throw new BusinessException("Impossible de s'envoyer un message a soi-meme.");
        }
        exiger(destinataireId); // le destinataire doit exister
        Utilisateur expediteur = exiger(expediteurId);
        Message message = messageRepository.save(Message.builder()
                .expediteur(expediteur)
                .destinataireId(destinataireId)
                .contenu(texte)
                .pieceJointeUrl(vide(pieceJointeUrl))
                .typePiece(vide(typePiece))
                .build());
        return MessageResponse.from(message);
    }

    /** Conversation privee entre l'utilisateur connecte et un autre membre. */
    @Transactional(readOnly = true)
    public List<MessageResponse> conversationPrivee(String utilisateurId, String autreId) {
        return messageRepository.conversationPrivee(utilisateurId, autreId).stream()
                .map(MessageResponse::from).toList();
    }

    /**
     * Valide et normalise le contenu. Un message peut etre vide de texte s'il
     * porte une piece jointe (image ou document).
     */
    private String preparer(String contenu, String pieceJointeUrl) {
        String texte = contenu == null ? "" : contenu.trim();
        boolean aPiece = pieceJointeUrl != null && !pieceJointeUrl.isBlank();
        if (texte.isEmpty() && !aPiece) {
            throw new BusinessException("Le message ne peut pas etre vide.");
        }
        if (texte.length() > LONGUEUR_MAX) {
            throw new BusinessException("Message trop long (max " + LONGUEUR_MAX + " caracteres).");
        }
        return texte;
    }

    /** Renvoie null pour une chaine vide/blanche (colonne laissee nulle). */
    private String vide(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }

    private Utilisateur exiger(String id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable : " + id));
    }
}
