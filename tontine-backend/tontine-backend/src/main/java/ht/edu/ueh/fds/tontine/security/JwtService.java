package ht.edu.ueh.fds.tontine.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Fabrique et verifie les jetons JWT (le « bracelet numerique »).
 * Le jeton contient l'identifiant de l'utilisateur (subject) et son role.
 */
@Service
public class JwtService {

    private final SecretKey cle;
    private final long dureeValiditeMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long dureeValiditeMs) {
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.dureeValiditeMs = dureeValiditeMs;
    }

    /** Genere un jeton pour un utilisateur (a la connexion). */
    public String genererToken(Utilisateur utilisateur) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + dureeValiditeMs);
        return Jwts.builder()
                .subject(utilisateur.getId())
                .claim("role", utilisateur.getRole())
                .claim("nom", utilisateur.getNom())
                .issuedAt(maintenant)
                .expiration(expiration)
                .signWith(cle)
                .compact();
    }

    /** Extrait l'identifiant de l'utilisateur depuis un jeton valide. */
    public String extraireUtilisateurId(String token) {
        return parser(token).getSubject();
    }

    /** Extrait le role depuis un jeton valide. */
    public String extraireRole(String token) {
        return parser(token).get("role", String.class);
    }

    /** Verifie la signature et la date d'expiration ; leve une exception si invalide. */
    public boolean estValide(String token) {
        try {
            parser(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parser(String token) {
        return Jwts.parser()
                .verifyWith(cle)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
