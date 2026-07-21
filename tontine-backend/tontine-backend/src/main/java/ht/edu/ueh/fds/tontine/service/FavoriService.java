package ht.edu.ueh.fds.tontine.service;

import ht.edu.ueh.fds.tontine.dto.TransfertDtos.FavoriResponse;
import ht.edu.ueh.fds.tontine.entity.FavoriBeneficiaire;
import ht.edu.ueh.fds.tontine.entity.Utilisateur;
import ht.edu.ueh.fds.tontine.exception.BusinessException;
import ht.edu.ueh.fds.tontine.repository.FavoriBeneficiaireRepository;
import ht.edu.ueh.fds.tontine.repository.UtilisateurRepository;
import ht.edu.ueh.fds.tontine.repository.VerificationKycRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Gestion des bénéficiaires favoris (transferts rapides). */
@Service
@RequiredArgsConstructor
public class FavoriService {

    private final FavoriBeneficiaireRepository favoriRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final VerificationKycRepository kycRepository;

    @Transactional(readOnly = true)
    public List<FavoriResponse> lister(String proprietaireId) {
        List<FavoriResponse> resultat = new ArrayList<>();
        for (FavoriBeneficiaire f : favoriRepository.findByProprietaireIdOrderByDateCreationDesc(proprietaireId)) {
            utilisateurRepository.findById(f.getBeneficiaireId()).ifPresent(u -> {
                boolean kyc = kycRepository.findByUtilisateurId(u.getId())
                        .map(k -> "APPROUVE".equals(k.getStatut())).orElse(false);
                resultat.add(new FavoriResponse(f.getId(), u.getId(), u.getUsername(),
                        u.getPrenom() + " " + u.getNom(), u.getPhotoUrl(), kyc));
            });
        }
        return resultat;
    }

    @Transactional
    public void ajouter(String proprietaireId, String beneficiaireId) {
        if (proprietaireId.equals(beneficiaireId)) {
            throw new BusinessException("Vous ne pouvez pas vous ajouter en favori.");
        }
        Utilisateur benef = utilisateurRepository.findById(beneficiaireId)
                .orElseThrow(() -> new BusinessException("Bénéficiaire introuvable."));
        if (favoriRepository.existsByProprietaireIdAndBeneficiaireId(proprietaireId, benef.getId())) {
            return; // déjà en favori
        }
        favoriRepository.save(FavoriBeneficiaire.builder()
                .id(UUID.randomUUID().toString())
                .proprietaireId(proprietaireId)
                .beneficiaireId(benef.getId())
                .dateCreation(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void supprimer(String proprietaireId, String beneficiaireId) {
        favoriRepository.findByProprietaireIdAndBeneficiaireId(proprietaireId, beneficiaireId)
                .ifPresent(favoriRepository::delete);
    }
}
