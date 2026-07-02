package ht.edu.ueh.fds.tontine.dto;

/** Reponse de connexion : le jeton JWT + les infos publiques de l'utilisateur. */
public record ConnexionResponse(
        String token,
        UtilisateurResponse utilisateur
) {
}
