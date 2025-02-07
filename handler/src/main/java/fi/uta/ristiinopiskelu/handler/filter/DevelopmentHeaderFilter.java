package fi.uta.ristiinopiskelu.handler.filter;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
@Profile("dev")
public class DevelopmentHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        MutableHttpServletRequest req = new MutableHttpServletRequest((HttpServletRequest) request);
        req.putHeader("SSL_CLIENT_S_DN_O", "UEF");
        req.putHeader("eppn", "testailija@testailija.fi");
        chain.doFilter(req, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }
}
