package app.mikraal.bff.web.directory;

import static app.mikraal.bff.web.SeedIds.ANA_FAMILY;
import static app.mikraal.bff.web.SeedIds.COUNCIL;
import static app.mikraal.bff.web.SeedIds.GROUP;
import static app.mikraal.bff.web.SeedIds.LUCIA;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import app.mikraal.bff.web.Sessions;
import app.mikraal.bff.web.WebTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebTest
class DirectoryEndpointTest {

    @Autowired
    MockMvc mvc;

    @Test
    void aScouterGetsTheGroupDirectoryWithoutFamilies() throws Exception {
        mvc.perform(get("/api/groups/{groupId}/directory", GROUP).session(Sessions.login(mvc, LUCIA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.people", hasSize(7)))
                .andExpect(jsonPath("$.people[*].id", not(hasItem(ANA_FAMILY.toString()))))
                .andExpect(jsonPath("$.positions", hasSize(7)))
                .andExpect(jsonPath("$.councils[0].id").value(COUNCIL.toString()))
                .andExpect(jsonPath("$.councils[0].name").value("Consejo de grupo de noviembre"))
                .andExpect(jsonPath("$.councils[0].date").value("2026-11-20"))
                .andExpect(jsonPath("$.events", hasSize(2)));
    }

    @Test
    void aFamilyMemberIsForbidden() throws Exception {
        mvc.perform(get("/api/groups/{groupId}/directory", GROUP).session(Sessions.login(mvc, ANA_FAMILY)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void anotherGroupIsForbidden() throws Exception {
        mvc.perform(get("/api/groups/{groupId}/directory", UUID.randomUUID()).session(Sessions.login(mvc, LUCIA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutASessionAnswers401() throws Exception {
        mvc.perform(get("/api/groups/{groupId}/directory", GROUP))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
