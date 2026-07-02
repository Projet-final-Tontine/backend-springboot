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
 * Test de bout en bout du parcours principal via l'API HTTP :
 * inscription -> activation admin -> creation d'un Sol -> adhesion via code.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ParcoursSolIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Test
    void parcoursComplet_inscriptionActivationCreationAdhesion() throws Exception {
        // 1. Inscription d'un administrateur
        String adminId = idDe(inscrire("Admin", "Sys", "50937000000",
                "admin@sol.ht", "001-000", "ADMIN"));

        // 2. Inscription d'une Manman sol (statut EN_ATTENTE au depart)
        MvcResult manmanRes = inscrire("Jean", "Marie", "50937111111",
                "manman@sol.ht", "001-111", "MANMAN_SOL");
        String manmanId = idDe(manmanRes);
        assertThat(champ(manmanRes, "statut")).isEqualTo("EN_ATTENTE");

        // 3. L'admin active le compte de la Manman sol
        MvcResult activation = mvc.perform(post("/api/admin/utilisateurs/" + manmanId + "/activer")
                        .header("X-User-Id", adminId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(champ(activation, "statut")).isEqualTo("ACTIF");

        // 4. La Manman sol cree un Sol -> un code d'invitation est genere
        MvcResult solRes = mvc.perform(post("/api/sols")
                        .header("X-User-Id", manmanId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"Sol du quartier","description":"Test",
                                 "nombreMaxMembres":5,"montantCotisation":1000.00,
                                 "frequence":"MENSUEL","dateDebut":"2026-07-01"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        String code = champ(solRes, "codeInvitation");
        assertThat(code).isNotBlank();

        // 5. Un membre s'inscrit puis rejoint le Sol grace au code
        String membreId = idDe(inscrire("Paul", "Pierre", "50937222222",
                "membre@sol.ht", "001-222", "MEMBRE"));

        mvc.perform(post("/api/sols/rejoindre")
                        .header("X-User-Id", membreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeInvitation\":\"" + code + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordrePassage").value(2)); // la Manman sol est en 1

        // 6. Un mauvais code est refuse proprement (400 + message metier)
        mvc.perform(post("/api/sols/rejoindre")
                        .header("X-User-Id", membreId)
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

    private String idDe(MvcResult res) throws Exception {
        return champ(res, "id");
    }

    private String champ(MvcResult res, String nom) throws Exception {
        JsonNode node = json.readTree(res.getResponse().getContentAsString());
        return node.get(nom).asText();
    }
}
