package app.mikraal.bff.web.http;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.HandlerInterceptor;

/** Writes must carry {@code X-Requested-With: mikraal-web}, which a cross-site form cannot send. */
class CsrfGuard implements HandlerInterceptor {

    static final String HEADER = "X-Requested-With";
    static final String EXPECTED = "mikraal-web";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!SAFE_METHODS.contains(request.getMethod()) && !EXPECTED.equals(request.getHeader(HEADER))) {
            throw new ErrorResponseException(HttpStatus.FORBIDDEN,
                    ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                            "Writes must send the header " + HEADER + ": " + EXPECTED + "."),
                    null);
        }
        return true;
    }
}
