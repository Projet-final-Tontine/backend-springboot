package ht.edu.ueh.fds.tontine.dto;

/**
 * Résultat de la vérification d'intégrité du Registre Inviolable.
 *
 * @param intacte          {@code true} si toute la chaîne est authentique
 * @param nombreBlocs      nombre total d'écritures scellées
 * @param positionRupture  position du premier bloc corrompu ({@code null} si intacte)
 * @param empreinteGlobale hash du dernier bloc = empreinte de tout le registre
 *                         ({@code null} si une rupture est détectée)
 * @param message          message lisible pour l'utilisateur
 */
public record VerificationRegistreResponse(
        boolean intacte,
        long nombreBlocs,
        Long positionRupture,
        String empreinteGlobale,
        String message) {
}
