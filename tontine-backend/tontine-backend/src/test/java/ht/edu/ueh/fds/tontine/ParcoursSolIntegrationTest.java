package ht.edu.ueh.fds.tontine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de bout en bout du parcours principal via l'API HTTP securisee (JWT) :
 * inscription -> connexion (jeton) -> activation admin -> creation d'un Sol
 * -> adhesion via code -> controles de securite.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ParcoursSolIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void parcoursComplet_avecAuthentificationJwt() throws Exception {
        // 1. Inscriptions
        inscrire("Admin", "Sys", "50937000000", "admin@sol.ht", "001-000", "ADMIN");
        MvcResult manmanRes = inscrire("Jean", "Marie", "50937111111",
                "manman@sol.ht", "001-111", "MANMAN_SOL");
        String manmanId = champ(manmanRes, "id");
        assertThat(champ(manmanRes, "statut")).isEqualTo("EN_ATTENTE");

        // 2. Connexions -> recuperation des jetons JWT
        String adminToken = connecter("50937000000", "motdepasse123");
        assertThat(adminToken).isNotBlank();

        // 3. L'admin active le compte de la Manman sol (endpoint reserve ADMIN)
        mvc.perform(post("/api/admin/utilisateurs/" + manmanId + "/activer")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIF"));

        // 4. La Manman sol se connecte et cree un Sol
        String manmanToken = connecter("50937111111", "motdepasse123");
        MvcResult solRes = mvc.perform(post("/api/sols")
                        .header("Authorization", "Bearer " + manmanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Sol du quartier","description":"Test",
                                 "nombreMaxMembres":5,"montantCotisation":1000.00,
                                 "frequence":"MENSUEL","dateDebut":"2026-07-01"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String code = champ(solRes, "codeInvitation");
        assertThat(code).isNotBlank();

        // 5. Un membre s'inscrit, se connecte et rejoint le Sol via le code
        inscrire("Paul", "Pierre", "50937222222", "membre@sol.ht", "001-222", "MEMBRE");
        String membreToken = connecter("50937222222", "motdepasse123");
        mvc.perform(post("/api/sols/rejoindre")
                        .header("Authorization", "Bearer " + membreToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeInvitation\":\"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordrePassage").value(2));

        // 6. SECURITE : sans jeton -> acces refuse (401)
        mvc.perform(post("/api/sols/rejoindre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeInvitation\":\"" + code + "\"}"))
                .andExpect(status().isUnauthorized());

        // 7. SECURITE : un membre ne peut pas acceder a un endpoint admin (403)
        mvc.perform(post("/api/admin/utilisateurs/" + manmanId + "/desactiver")
                        .header("Authorization", "Bearer " + membreToken))
                .andExpect(status().isForbidden());

        // 8. Un mauvais code est refuse proprement (400 + message metier)
        mvc.perform(post("/api/sols/rejoindre")
                        .header("Authorization", "Bearer " + membreToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeInvitation\":\"XXXXXXXX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Code d'invitation invalide."));
    }

    private MvcResult inscrire(String nom, String prenom, String tel,
                               String email, String cin, String role) throws Exception {
        return mvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"%s","prenom":"%s","sexe":"M","telephone":"%s",
                                 "email":"%s","adresse":"Port-au-Prince","cinNif":"%s",
                                 "dateNaissance":"1990-01-01","motDePasse":"motdepasse123","role":"%s"}"""
                                .formatted(nom, prenom, tel, email, cin, role)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String connecter(String telephone, String motDePasse) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telephone\":\"" + telephone + "\",\"motDePasse\":\"" + motDePasse + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return champ(res, "token");
    }

    private String champ(MvcResult res, String nom) throws Exception {
        JsonNode node = json.readTree(res.getResponse().getContentAsString());
        return node.get(nom).asText();
    }
}
