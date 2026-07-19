package ht.edu.ueh.fds.tontine.repository;

import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, String> {

    /** Connexion : l'identifiant principal est le numero de telephone (lie a Mon Cash). */
    Optional<Utilisateur> findByTelephone(String telephone);

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByTelephone(String telephone);

    boolean existsByEmail(String email);

    boolean existsByCinNif(String cinNif);

    /** Recherche publique par username (insensible à la casse). */
    Optional<Utilisateur> findByUsernameIgnoreCase(String username);

    /** Disponibilité d'un username (insensible à la casse). */
    boolean existsByUsernameIgnoreCase(String username);

    /** Recherche d'un bénéficiaire par e-mail (insensible à la casse). */
    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    /** Pour le tableau de bord admin : filtrer par statut (ACTIF, BLOQUE, ...). */
    List<Utilisateur> findByStatut(String statut);

    /** Purge automatique : comptes sans connexion depuis une date donnee (> 1 an). */
    List<Utilisateur> findByDerniereConnexionBefore(LocalDateTime dateLimite);
}
