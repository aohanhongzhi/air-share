package hxy.dragon.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.PrintWriter;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
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
                        .requestMatchers("/file/delete/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            // FIXME 没有生效，等chatgpt6来解决
                            // Check if it's an AJAX request
                            String accept = request.getHeader("Accept");
                            String contentType = request.getHeader("Content-Type");

                            if (accept != null && accept.contains("application/json") ||
                                    (contentType != null && contentType.contains("application/json"))) {
                                // Handle AJAX request - return JSON
                                response.setContentType("application/json;charset=utf-8");
                                PrintWriter out = response.getWriter();
                                response.setStatus(200);
                                out.write("{\"success\": true, \"message\": \"登录成功\", \"user\": " +
                                        new ObjectMapper().writeValueAsString(authentication.getPrincipal()) + "}");
                                out.flush();
                                out.close();
                            } else {
                                // Handle form submission - redirect to home page
                                response.sendRedirect("/?login=success");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            // FIXME 没有生效，等chatgpt6来解决
                            // Check if it's an AJAX request
                            String accept = request.getHeader("Accept");
                            String contentType = request.getHeader("Content-Type");

                            if (accept != null && accept.contains("application/json") ||
                                    (contentType != null && contentType.contains("application/json"))) {
                                // Handle AJAX request - return JSON
                                response.setContentType("application/json;charset=utf-8");
                                PrintWriter out = response.getWriter();
                                response.setStatus(401);
                                out.write("{\"success\": false, \"error\": \"" + exception.getMessage() + "\"}");
                                out.flush();
                                out.close();
                            } else {
                                // Handle form submission - redirect back to login page with error
                                response.sendRedirect("/login?login=error");
                            }
                        })
                        .permitAll()
                )
                // JWT过滤器在表单登录之前处理，这样JWT认证的请求不会被表单登录处理器拦截
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}