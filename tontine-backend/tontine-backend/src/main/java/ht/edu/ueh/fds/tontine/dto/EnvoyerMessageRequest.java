package ht.edu.ueh.fds.tontine.dto;

/**
 * Corps de requete pour envoyer un message.
 * {@code pieceJointeUrl} et {@code typePiece} sont optionnels (message texte simple).
 */
public record EnvoyerMessageRequest(String contenu, String pieceJointeUrl, String typePiece) {
}
