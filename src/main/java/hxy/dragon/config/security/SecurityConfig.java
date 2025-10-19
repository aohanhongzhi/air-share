package hxy.dragon.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration
 *
 * @author houxiaoyi
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    @Lazy
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {

            // 获取 header? 决定302还是 401？

            // For template pages (like /files), redirect to login
//            response.sendRedirect("/login");

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Unauthorized\"}");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .formLogin(form -> form.disable()) // 禁用表单登录（避免 302）
                .httpBasic(httpBasic -> httpBasic.disable()) // 如果不需要 Basic
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/", "/index", "/index.html").permitAll()
                        .requestMatchers("/login", "/register").permitAll()
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/lib/**", "/images/**").permitAll()
                        .requestMatchers("/file/download/**").permitAll() // Allow public file downloads
                        // Protected endpoints - require authentication
                        .requestMatchers("/files").authenticated() // Require authentication to view files page (supports session auth)
                        .requestMatchers("/file/list").authenticated() // Require authentication to get file list (API only)
                        .requestMatchers("/file/del").authenticated() // Protect file deletion via web interface (supports session auth)
                        .requestMatchers("/file/upload").authenticated()
                        .requestMatchers("/file/rename").authenticated()
                        .requestMatchers("/file/delete/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}