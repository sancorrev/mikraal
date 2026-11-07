package app.mikraal.bff.web.http;

import app.mikraal.bff.web.identity.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.HandlerInterceptor;

/** Rejects calls without a session with 401. */
class SessionGuard implements HandlerInterceptor {

    private final Identity identity;

    SessionGuard(Identity identity) {
        this.identity = identity;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (identity.currentPersonId().isEmpty()) {
            throw new ErrorResponseException(HttpStatus.UNAUTHORIZED);
        }
        return true;
    }
}
