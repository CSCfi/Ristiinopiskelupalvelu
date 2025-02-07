package fi.csc.ristiinopiskelu.admin.config.profiles.dev;

import com.google.common.collect.ImmutableList;
import fi.csc.ristiinopiskelu.admin.config.WebSecurityConfig;
import fi.csc.ristiinopiskelu.admin.security.ShibbolethLogoutSuccessHandler;
import fi.csc.ristiinopiskelu.admin.security.profiles.dev.DevelopmentShibbolethMockAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.RequestAttributeAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Profile("dev")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class DevelopmentWebSecurityConfig extends WebSecurityConfig {

    @Bean
    @Override
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(new ShibbolethLogoutSuccessHandler(getHakaLogoutUrl()))
                        .permitAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/login").permitAll()
                    .requestMatchers("/**").hasAnyRole("ADMIN", "SUPERUSER"))

                // add a mock authentication filter that simulates a succesful Shibboleth authentication
                .addFilterBefore(new DevelopmentShibbolethMockAuthenticationFilter(), RequestAttributeAuthenticationFilter.class)
                .build();
    }

    @Override
    protected CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ImmutableList.of("*"));
        config.setAllowedMethods(ImmutableList.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
