package app.mikraal.bff.web.identity;

import static app.mikraal.bff.web.SeedIds.ANA_FAMILY;
import static app.mikraal.bff.web.SeedIds.GROUP;
import static app.mikraal.bff.web.SeedIds.LUCIA;
import static app.mikraal.bff.web.SeedIds.MARTA;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.mikraal.bff.web.Sessions;
import app.mikraal.bff.web.WebTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebTest
class SessionEndpointsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listsTheSyntheticPeopleWithTheirPositions() throws Exception {
        mvc.perform(get("/api/dev/people"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(8)))
                .andExpect(jsonPath("$.items[?(@.id == '%s')].isScouter".formatted(ANA_FAMILY)).value(false))
                .andExpect(jsonPath("$.items[?(@.id == '%s')].name".formatted(MARTA)).value("Marta Gómez Vidal"))
                .andExpect(jsonPath("$.items[?(@.id == '%s')].positions[*]".formatted(MARTA))
                        .value(containsInAnyOrder("Coordinación de sección · Manada", "Secretaría")));
    }

    @Test
    void startsReadsAndEndsASession() throws Exception {
        MockHttpSession session = Sessions.login(mvc, LUCIA);

        mvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.id").value(LUCIA.toString()))
                .andExpect(jsonPath("$.person.name").value("Lucía Martín Ortega"))
                .andExpect(jsonPath("$.groups", hasSize(1)))
                .andExpect(jsonPath("$.groups[0].id").value(GROUP.toString()))
                .andExpect(jsonPath("$.groups[0].name").value("Grupo Scout Kraal Ejemplo"))
                .andExpect(jsonPath("$.groups[0].timeZone").value("Europe/Madrid"));

        mvc.perform(delete("/api/session").session(session).header("X-Requested-With", "mikraal-web"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/session").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aFamilyMemberHasNoGroups() throws Exception {
        mvc.perform(get("/api/session").session(Sessions.login(mvc, ANA_FAMILY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups", hasSize(0)));
    }

    @Test
    void withoutASessionAnswers401() throws Exception {
        mvc.perform(get("/api/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void anUnknownPersonCannotLogIn() throws Exception {
        mvc.perform(post("/api/session/dev-login")
                        .header("X-Requested-With", "mikraal-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":\"0193ffff-0000-7000-8000-000000000000\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
