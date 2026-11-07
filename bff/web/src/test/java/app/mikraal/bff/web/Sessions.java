package app.mikraal.bff.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

public final class Sessions {

    private Sessions() {
    }

    /** Logs in through the dev login and returns the session. */
    public static MockHttpSession login(MockMvc mvc, UUID personId) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/session/dev-login")
                        .header("X-Requested-With", "mikraal-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":\"" + personId + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }
}
