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
        inscrire("Admin", "Sys", "50937000000", "admin@sol.ht", "admin_sys", "ADMIN");
        MvcResult manmanRes = inscrire("Jean", "Marie", "50937111111",
                "manman@sol.ht", "jean_marie", "MANMAN_SOL");
        String manmanId = champ(manmanRes, "id");
        // Auto-activation a l'inscription : le compte est directement ACTIF
        // (l'administrateur conserve le pouvoir de le bloquer ensuite).
        assertThat(champ(manmanRes, "statut")).isEqualTo("ACTIF");

        // 2. Connexions -> recuperation des jetons JWT
        String adminToken = connecter("50937000000", "motdepasse123");
        assertThat(adminToken).isNotBlank();

        // 3. L'admin confirme l'activation du compte (endpoint reserve ADMIN) : reste ACTIF
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

        // 5. Un membre s'inscrit, se connecte et demande a rejoindre le Sol via le code.
        //    L'adhesion passe par une demande EN_ATTENTE : l'ordre de passage
        //    n'est attribue qu'apres approbation par la Manman sol.
        inscrire("Paul", "Pierre", "50937222222", "membre@sol.ht", "paul_pierre", "MEMBRE");
        String membreToken = connecter("50937222222", "motdepasse123");
        MvcResult demandeRes = mvc.perform(post("/api/sols/rejoindre")
                        .header("Authorization", "Bearer " + membreToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeInvitation\":\"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statutMembre").value("EN_ATTENTE"))
                .andReturn();
        String membreSolId = champ(demandeRes, "id");

        // 5b. La Manman sol approuve la demande : le membre devient ACTIF, ordre 2.
        mvc.perform(post("/api/sols/membres/" + membreSolId + "/approuver")
                        .header("Authorization", "Bearer " + manmanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutMembre").value("ACTIF"))
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
                               String email, String username, String role) throws Exception {
        return mvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"%s","prenom":"%s","sexe":"M","telephone":"%s",
                                 "email":"%s","username":"%s","adresse":"Port-au-Prince",
                                 "dateNaissance":"1990-01-01","motDePasse":"motdepasse123","role":"%s"}"""
                                .formatted(nom, prenom, tel, email, username, role)))
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
