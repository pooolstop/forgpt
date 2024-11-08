package top.poools.coreproxy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import top.poools.coreproxy.properties.CoreProxyProperties;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

  private static final String[] AUTH_WHITELIST = {
          "/actuator/**",
          "/admin/**",
  };
  private final CoreProxyProperties properties;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorizeHttpRequests ->
                    authorizeHttpRequests
                            .requestMatchers(AUTH_WHITELIST).permitAll()
                            .anyRequest().authenticated())
            .httpBasic(config -> config.authenticationEntryPoint(new CustomAuthenticationEntryPoint()))
            .build();
  }

  @Bean
  public InMemoryUserDetailsManager userDetailsService() {
    UserDetails user = User
            .withUsername(properties.getSecurity().getBasicAuthLogin())
            .password(passwordEncoder().encode(properties.getSecurity().getBasicAuthPassword()))
            .build();
    return new InMemoryUserDetailsManager(user);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(8);
  }
}