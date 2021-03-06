package com.cfgglobal.test.security.handlers;


import com.cfgglobal.test.cache.CacheClient;
import com.cfgglobal.test.config.app.ApplicationProperties;
import com.cfgglobal.test.domain.User;
import com.cfgglobal.test.security.TokenHelper;
import com.cfgglobal.test.security.UserTokenState;
import com.cfgglobal.test.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Option;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@EnableConfigurationProperties(value = {ApplicationProperties.class})
@Component
public class AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("spring.profiles.active")
    String profile;
    @Autowired
    TokenHelper tokenHelper;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ApplicationProperties applicationProperties;


    @Autowired
    UserService userService;

    @Autowired
    CacheClient cacheClient;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        clearAuthenticationAttributes(request);
        User user = (User) authentication.getPrincipal();

        if (!"local".equals(profile)) {
            cacheClient.set(applicationProperties.getUserClass() + "-" + user.getUsername(), userService.getUserWithPermissions(user.getUsername()));
        }

        String jws = tokenHelper.generateToken(user.getUsername());

        Cookie authCookie = new Cookie(applicationProperties.getJwt().getCookie(), (jws));
        authCookie.setPath("/");
        authCookie.setHttpOnly(true);
        authCookie.setMaxAge(applicationProperties.getJwt().getExpiresIn().intValue());

        Cookie userCookie = new Cookie(applicationProperties.getUserCookie(), (user.getUsername()));
        userCookie.setPath("/");
        userCookie.setMaxAge(applicationProperties.getJwt().getExpiresIn().intValue());

        response.addCookie(authCookie);
        response.addCookie(userCookie);

        UserTokenState userTokenState = new UserTokenState();
        userTokenState.setAccess_token(jws);
        userTokenState.setExpires_in(applicationProperties.getJwt().getExpiresIn());
        userTokenState.setType(Option.of(user.getUserType()).map(Enum::name).getOrElse(""));

        String jwtResponse = objectMapper.writeValueAsString(userTokenState);
        response.setContentType("application/json");
        response.getWriter().write(jwtResponse);
    }
}
