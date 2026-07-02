package ht.edu.ueh.fds.tontine.dto;

import ht.edu.ueh.fds.tontine.entity.Utilisateur;

/** Vue publique d'un utilisateur (jamais le mot de passe). */
public record UtilisateurResponse(
        String id,
        String nom,
        String prenom,
        String telephone,
        String email,
        String photoUrl,
        String role,
        String statut
) {
    public static UtilisateurResponse from(Utilisateur u) {
        return new UtilisateurResponse(
                u.getId(), u.getNom(), u.getPrenom(), u.getTelephone(),
                u.getEmail(), u.getPhotoUrl(), u.getRole(), u.getStatut());
    }
}
