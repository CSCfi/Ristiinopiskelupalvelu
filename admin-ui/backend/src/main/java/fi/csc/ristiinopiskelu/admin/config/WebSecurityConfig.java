package fi.csc.ristiinopiskelu.admin.config;

import com.google.common.collect.ImmutableList;
import fi.csc.ristiinopiskelu.admin.security.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Profile("!dev")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Value("${ristiinopiskelupalvelu.hakaLogoutUrl}")
    private String hakaLogoutUrl;

    protected String getHakaLogoutUrl() {
        return hakaLogoutUrl;
    }

    @Bean
    public ShibbolethAuthenticationUserDetailsService shibbolethAuthenticationUserDetailsService() {
        return new ShibbolethAuthenticationUserDetailsService();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(ShibbolethAuthenticationUserDetailsService shibbolethAuthenticationUserDetailsService) {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(shibbolethAuthenticationUserDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public ShibbolethAuthenticationDetailsSource shibbolethAuthenticationDetailsSource() {
        return new ShibbolethAuthenticationDetailsSource();
    }
    
    @Bean
    public FilterRegistrationBean<RequestAttributeAuthenticationFilter> requestAttributeAuthenticationFilter(AuthenticationManager authenticationManager,
                                                                                                             ShibbolethAuthenticationDetailsSource shibbolethAuthenticationDetailsSource) {
        RequestAttributeAuthenticationFilter requestAttributeAuthenticationFilter = new RequestAttributeAuthenticationFilter();
        requestAttributeAuthenticationFilter.setAuthenticationManager(authenticationManager);
        requestAttributeAuthenticationFilter.setPrincipalEnvironmentVariable(ShibbolethAttribute.EPPN.getValue());
        requestAttributeAuthenticationFilter.setAuthenticationDetailsSource(shibbolethAuthenticationDetailsSource);
        requestAttributeAuthenticationFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/login"));
        requestAttributeAuthenticationFilter.setAuthenticationSuccessHandler(new ShibbolethAuthenticationSuccessHandler());
        requestAttributeAuthenticationFilter.setAuthenticationFailureHandler(new ShibbolethAuthenticationFailureHandler());
        return new FilterRegistrationBean<>(requestAttributeAuthenticationFilter);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        XorCsrfTokenRequestAttributeHandler csrfTokenRequestHandlerDelegate = new XorCsrfTokenRequestAttributeHandler();
        csrfTokenRequestHandlerDelegate.setCsrfRequestAttributeName("_csrf");

        // delegate the handle() method to XorCsrfTokenRequestAttributeHandler as recommended in Spring migration guide:
        // https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html#_i_am_using_angularjs_or_another_javascript_framework
        CsrfTokenRequestHandler csrfTokenRequestHandler = csrfTokenRequestHandlerDelegate::handle;

        return http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfTokenRequestHandler))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(new ShibbolethLogoutSuccessHandler(hakaLogoutUrl))
                        .permitAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/**").hasAnyRole("ADMIN", "SUPERUSER"))
                // manually disable CSRF token deferring since our UI doesn't currently support it, see migration guide link above
                .addFilterAfter(new CsrfCookieFilter(), RequestAttributeAuthenticationFilter.class)
                .build();
    }
    
    protected CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ImmutableList.of("http://localhost"));
        config.setAllowedMethods(ImmutableList.of(CorsConfiguration.ALL));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            // render the token value to a cookie by causing the deferred token to be loaded
            csrfToken.getToken();
            filterChain.doFilter(request, response);
        }
    }
}
