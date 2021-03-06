package fi.essentia.somacms.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Solves the problem of Spring Security forwarding an AJAX request to the login page.
 *
 * Thanks to http://yoyar.com/blog/2012/06/dealing-with-the-spring-security-ajax-session-timeout-problem/
 */
public class AjaxAwareAuthenticationEntryPoint
        extends LoginUrlAuthenticationEntryPoint {

    public AjaxAwareAuthenticationEntryPoint(String loginUrl) {
        super(loginUrl);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        boolean isAjax = request.getRequestURI().startsWith("/admin/api/");

        if (isAjax) {
            response.sendError(403, "Forbidden");
        } else {
            super.commence(request, response, authException);
        }
    }
}