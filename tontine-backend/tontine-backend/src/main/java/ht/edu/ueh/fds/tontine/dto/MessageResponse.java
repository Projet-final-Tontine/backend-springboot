package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.Message;

import java.time.LocalDateTime;

/** Vue d'un message de discussion (avec le nom et la photo de l'expediteur). */
public record MessageResponse(
        String id,
        String expediteurId,
        String expediteurNom,
        String expediteurPhoto,
        String contenu,
        String pieceJointeUrl,
        String typePiece,
        LocalDateTime dateEnvoi
) {
    public static MessageResponse from(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getExpediteur().getId(),
                m.getExpediteur().getPrenom() + " " + m.getExpediteur().getNom(),
                m.getExpediteur().getPhotoUrl(),
                m.getContenu(),
                m.getPieceJointeUrl(),
                m.getTypePiece(),
                m.getDateEnvoi());
    }
}
