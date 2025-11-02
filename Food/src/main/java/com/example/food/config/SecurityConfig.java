package com.example.food.config;

import com.example.food.security.JwtAuthenticationEntryPoint;
import com.example.food.security.JwtAuthenticationFilter;
import com.example.food.config.RequestLoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RequestLoggingFilter requestLoggingFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true); // Important for Authorization header

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll() // Allow public access to products and categories
                        .requestMatchers("/api/coupons/validate/**").permitAll() // Allow coupon validation
                        .requestMatchers("/api/coupons/active").permitAll() // Allow active coupons
                        .requestMatchers(HttpMethod.POST, "/api/orders/calculate-shipping").permitAll() // Allow shipping calculation
                        .requestMatchers(HttpMethod.POST, "/api/orders").authenticated() // Require authentication for order creation
                        .requestMatchers("/api/orders/test-auth").permitAll() // Allow test auth endpoint
                        .requestMatchers(HttpMethod.POST, "/api/orders/test-create").permitAll() // Allow test create endpoint
                        .requestMatchers("/api/orders/**").permitAll() // Allow public access to other order endpoints
                        .requestMatchers("/admin/products/*/options").permitAll() // Allow public access to product options
                        .requestMatchers(HttpMethod.POST, "/admin/products/create-test-product-with-options").permitAll() // Allow test endpoint
                        .requestMatchers("/api/user/test").permitAll() // Allow test endpoint
                        .requestMatchers("/api/test/**").permitAll() // Allow test endpoints
                        .requestMatchers("/api/app/**").permitAll() // Allow app APIs
                        // VNPay return callback must be public (VNPay server does not send JWT)
                        .requestMatchers(HttpMethod.GET, "/api/payment/payment-callback").permitAll()
                        .requestMatchers("/admin/store-location").permitAll() // Allow simple store location page
                        .requestMatchers("/admin/shipping-fees").permitAll() // Allow shipping fees page
                        .requestMatchers("/admin/shipping-fees/api/**").permitAll() // Allow shipping fees API
                        .requestMatchers("/ws/**").permitAll() // Allow WebSocket connections
                        .requestMatchers("/uploads/**").permitAll() // Allow access to uploaded files
                        .requestMatchers("/images/**").permitAll() // Allow access to images
                        .requestMatchers("/css/**").permitAll() // Allow access to CSS
                        .requestMatchers("/js/**").permitAll() // Allow access to JS
                        .requestMatchers("/admin/auth/**").permitAll() // Allow admin auth endpoints
                        .requestMatchers("/favicon.ico", "/error").permitAll() // Allow favicon and error pages
                        .requestMatchers("/admin/**").authenticated() // Require authentication for admin (not specific role)
                        .requestMatchers("/").permitAll()
                        // Protected endpoints
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/admin/auth/login")
                        .loginProcessingUrl("/admin/auth/login")
                        .usernameParameter("email") // Sử dụng email làm username
                        .passwordParameter("password")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/auth/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/auth/logout")
                        .logoutSuccessUrl("/admin/auth/login")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/api/auth/oauth2/success", true)
                );

        // Add request logging filter first (to log before authentication)
        http.addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
