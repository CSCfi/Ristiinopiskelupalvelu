package fi.csc.ristiinopiskelu.admin.config.profiles.dev;

import com.google.common.collect.ImmutableList;
import fi.csc.ristiinopiskelu.admin.config.WebSecurityConfig;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethAuthenticationFilter;
import fi.csc.ristiinopiskelu.admin.security.profiles.dev.DevelopmentShibbolethAuthenticationFilter;
import fi.csc.ristiinopiskelu.admin.security.profiles.dev.DevelopmentShibbolethAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Profile("dev")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class DevelopmentWebSecurityConfig extends WebSecurityConfig {

    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new DevelopmentShibbolethAuthenticationProvider();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider());
    }

    @Bean
    public ShibbolethAuthenticationFilter shibbolethAuthenticationFilter() {

        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl("/");

        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setDefaultFailureUrl("/error");

        DevelopmentShibbolethAuthenticationFilter filter = new DevelopmentShibbolethAuthenticationFilter("/login/**");
        filter.setAuthenticationSuccessHandler(successHandler);
        filter.setAuthenticationFailureHandler(failureHandler);

        return filter;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        // forward straight to /login -> automagic login always
        http.anonymous().disable();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ImmutableList.of("*"));
        config.setAllowedMethods(ImmutableList.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
