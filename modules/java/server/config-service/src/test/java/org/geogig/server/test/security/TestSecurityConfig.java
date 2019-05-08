package org.geogig.server.test.security;

import org.geogig.server.service.auth.TestOnlyUserDetailsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAutoConfiguration
@EnableWebSecurity
@Slf4j
public class TestSecurityConfig extends WebSecurityConfigurerAdapter {

    private @Autowired BasicAuthenticationEntryPoint basicAuthenticationPoint;

    private @Autowired TestOnlyUserDetailsManager userDetailsManager;

    public @Bean static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public @Bean @Autowired TestOnlyUserDetailsManager userDetailsManager(
            PasswordEncoder pencoder) {
        TestOnlyUserDetailsManager manager = new TestOnlyUserDetailsManager(pencoder);
        return manager;
    }

    public @Bean BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("geogig web api");
        return entryPoint;
    }

    public @Autowired void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        auth.userDetailsService(userDetailsManager);

        if (!userDetailsManager.userExists("admin")) {
            UserDetails admin = User.builder().username("admin").password("g30g1g")
                    .roles("ADMIN", "USER").accountExpired(false).accountLocked(false)
                    .credentialsExpired(false).disabled(false)//
                    .build();
            userDetailsManager.createUser(admin);
            log.info("Created default admin user. This is for testing/development only!!!");
        }
    }

    protected @Override void configure(HttpSecurity http) throws Exception {
        http.antMatcher("/**").csrf().disable();
        http.authorizeRequests().antMatchers(HttpMethod.OPTIONS, "**").permitAll();// allow CORS
                                                                                   // option calls

        http.authorizeRequests()//
                .anyRequest()//
                .authenticated()//
                .antMatchers("/**")//
                .hasAnyRole("USER", "ADMIN")//
                .and()//
                .httpBasic().authenticationEntryPoint(basicAuthenticationPoint);
    }
}
