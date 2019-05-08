package org.geogig.server.app.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

//@Configuration
// @RestController
//@EnableWebSecurity
// @EnableOAuth2Client
// @EnableAuthorizationServer
//@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    // @Autowired
    // OAuth2ClientContext oauth2ClientContext;

    @Autowired
    private BasicAuthenticationEntryPoint basicAuthenticationPoint;

    public @Bean BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("geogig web api");
        return entryPoint;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> configurer;

        configurer = auth.inMemoryAuthentication();
        configurer.passwordEncoder(NoOpPasswordEncoder.getInstance());

        configurer.withUser("gabe").password("g30g1g").roles("USER");
        configurer.withUser("erik").password("g30g1g").roles("USER");
        configurer.withUser("dave").password("g30g1g").roles("USER");
        configurer.withUser("admin").password("g30g1g").roles("ADMIN", "USER");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        /*
        http.antMatcher("/**")
            .authorizeRequests()
                //landing page and OAuth2 login links
                .antMatchers("/", "/login**","/me",
                //JavaScript resources
                "/webjars/**",
                //swagger documentation
                "/docs/**", "/swagger-ui.html", "/swagger-resources/**", "/api-docs/**"
                ).permitAll()
            .anyRequest()
            .authenticated().and().exceptionHandling()
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/")).and().logout()
            .logoutSuccessUrl("/").permitAll().and()
            .csrf()
            .disable() //REVISIT: diabling cause getting errors wiht the java client
            //.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).and()
            .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class)
            .httpBasic()//
            .and().formLogin();
        */
        // @formatter:on

        // @formatter:off
//        http.authorizeRequests().antMatchers("/securityNone").permitAll()
//            .anyRequest().authenticated().and().httpBasic()
//            .authenticationEntryPoint(basicAuthenticationPoint);
        // @formatter:on

        http.antMatcher("/**").csrf().disable();
        http.authorizeRequests()//
                .anyRequest()//
                .authenticated()//
                .antMatchers("/**")//
                .hasAnyRole("USER", "ADMIN")//
                .and()//
                .httpBasic().authenticationEntryPoint(basicAuthenticationPoint);

    }

    // @formatter:off
    /*
    @RequestMapping(produces = "application/json", path = "/me")
    public Map<String, String> user(Principal principal) {
        if (principal == null) {
            throw new IllegalSelectorException();
        }

        Map<String, String> map = new LinkedHashMap<>();
        String fullName = null;
        if (principal instanceof OAuth2Authentication) {
            OAuth2Authentication oa2 = (OAuth2Authentication) principal;
            if (oa2.getUserAuthentication().getDetails() instanceof Map) {
                fullName = (String) ((Map) oa2.getUserAuthentication().getDetails()).get("name");
            }
        }

        String name = principal.getName();
        map.put("name", name);
        map.put("fullName", fullName);

        return map;
    }

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
        @Override
        public void configure(HttpSecurity http) throws Exception {
            // @formatter:off
            http.antMatcher("/me").authorizeRequests().anyRequest().authenticated();
            // @formatter:on
        }
    }

    @Bean
    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }

    @Bean
    @ConfigurationProperties("github")
    public ClientResources github() {
        return new ClientResources();
    }

    @Bean
    @ConfigurationProperties("facebook")
    public ClientResources facebook() {
        return new ClientResources();
    }

    private Filter ssoFilter() {
        CompositeFilter filter = new CompositeFilter();
        List<Filter> filters = new ArrayList<>();
        filters.add(ssoFilter(facebook(), "/login/facebook"));
        filters.add(ssoFilter(github(), "/login/github"));
        filter.setFilters(filters);
        return filter;
    }

    private Filter ssoFilter(ClientResources client, String path) {
        OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter(
                path);
        OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(),
                oauth2ClientContext);
        filter.setRestTemplate(template);
        UserInfoTokenServices tokenServices = new UserInfoTokenServices(
                client.getResource().getUserInfoUri(), client.getClient().getClientId());
        tokenServices.setRestTemplate(template);
        filter.setTokenServices(tokenServices);
        return filter;
    }
    class ClientResources {
    
        @NestedConfigurationProperty
        private AuthorizationCodeResourceDetails client = new AuthorizationCodeResourceDetails();
    
        @NestedConfigurationProperty
        private ResourceServerProperties resource = new ResourceServerProperties();
    
        public AuthorizationCodeResourceDetails getClient() {
            return client;
        }
    
        public ResourceServerProperties getResource() {
            return resource;
        }
    }    
    */
    // @formatter:on
}
