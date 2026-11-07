package app.mikraal.bff.web.http;

import static app.mikraal.bff.web.SeedIds.GROUP;
import static app.mikraal.bff.web.SeedIds.LUCIA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.ResultActions;

@WebTest
class CsrfGuardTest {

    @Autowired
    MockMvc mvc;

    @Test
    void rejectsAWriteWithoutTheHeader() throws Exception {
        MockHttpSession session = Sessions.login(mvc, LUCIA);

        expectForbidden(mvc.perform(post("/api/groups/{groupId}/tasks", GROUP).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Comprar tiendas\"}")));
    }

    @Test
    void rejectsAWriteWithAnotherValue() throws Exception {
        MockHttpSession session = Sessions.login(mvc, LUCIA);

        expectForbidden(mvc.perform(delete("/api/session").session(session)
                .header("X-Requested-With", "XMLHttpRequest")));
    }

    @Test
    void guardsTheDevLoginToo() throws Exception {
        expectForbidden(mvc.perform(post("/api/session/dev-login")
                .contentType(MediaType.APPLICATION_JSON).content("{\"personId\":\"" + LUCIA + "\"}")));
    }

    private static void expectForbidden(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Writes must send the header X-Requested-With: mikraal-web."));
    }
}
