package com.cfgglobal.test.security;

import com.cfgglobal.test.base.ApiResp;
import com.cfgglobal.test.config.ActionReportProperties;
import com.cfgglobal.test.config.app.ApplicationProperties;
import com.cfgglobal.test.config.jpa.SecurityAuditor;
import com.cfgglobal.test.domain.VisitRecord;
import com.cfgglobal.test.service.VisitRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;

import static com.cfgglobal.test.service.VisitRecordService.THRESHOLD;


@Slf4j
@EnableConfigurationProperties(value = {ApplicationProperties.class, ActionReportProperties.class})
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROOT_MATCHER = "/";
    private static final String FAVICON_MATCHER = "/favicon.ico";
    private static final String HTML_MATCHER = "/**/*.html";
    private static final String CSS_MATCHER = "/**/*.css";
    private static final String JS_MATCHER = "/**/*.js";
    private static final String IMG_MATCHER = "/images/*";
    private static final String LOGIN_MATCHER = "/login";
    private static final String LOGOUT_MATCHER = "/logout";
    @Autowired
    SecurityAuditor securityAuditor;
    @Autowired
    TokenHelper tokenHelper;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ApplicationProperties applicationProperties;
    @Autowired
    UserDetailsService userDetailsService;
    @Autowired
    private ActionReportProperties actionReportProperties;
    @Autowired
    private VisitRecordService visitRecordService;

    private static String getClientIp(HttpServletRequest request) {
        return Option.of(request.getHeader("X-Forwarded-For")).getOrElse(request.getRemoteAddr());
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        long start = Instant.now().getEpochSecond();
        AuthenticationRequestWrapper wrapRequest = new AuthenticationRequestWrapper(request);
        List<String> pathsToSkip = Option.of(applicationProperties.getJwt().getAnonymousUrls())
                .map(url -> url.split(","))
                .map(List::of)
                .getOrElse(List.empty());

        pathsToSkip = pathsToSkip.appendAll(List.of(
                ROOT_MATCHER,
                HTML_MATCHER,
                FAVICON_MATCHER,
                CSS_MATCHER,
                JS_MATCHER,
                IMG_MATCHER,
                LOGIN_MATCHER,
                LOGOUT_MATCHER,
                "/v1/payment/*",
                "/v1/code/*",
                "/sys/*",
                "/files/*",
                "/images/mail/*",
                "/v1/transaction/*/receipt", //for email
                "/v1/payment/*",
                "/less/*",
                "/less/material/*",
                "/images/payment/*"

        ));

        String authToken = tokenHelper.getToken(request);
        if (skipPathRequest(request, pathsToSkip)) {
            SecurityContextHolder.getContext().setAuthentication(new AnonAuthentication());
            chain.doFilter(wrapRequest, response);
        } else if (authToken != null && !authToken.equals("null") && !authToken.equals("undefined")) {
            String username = tokenHelper.getUsernameFromToken(authToken);
            if (username == null) {
                log.error("username is null , token {}", authToken);
                loginExpired(request, response);
            } else {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                TokenBasedAuthentication authentication = new TokenBasedAuthentication(userDetails);
                authentication.setToken(authToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                chain.doFilter(wrapRequest, response);
            }
        } else {
            System.out.println("URI" + request.getRequestURI());
            loginExpired(request, response);
        }

        if (actionReportProperties.isFirewall()) {
            if(visitRecordService.hasTooManyRequest(securityAuditor.getCurrentAuditor(), getClientIp(request))){
                ApiResp apiResp = new ApiResp();
                apiResp.setError(THRESHOLD + " requests allowed per min, if you need more, please contact us.");
                String msg = objectMapper.writeValueAsString(apiResp);
                response.setStatus(429);
                response.getWriter().write(msg);
            }
        }

        if (actionReportProperties.isVisitRecord()) {
            long end = Instant.now().getEpochSecond();
            VisitRecord visitRecord = new VisitRecord()
                    .setIp(getClientIp(request))
                    .setMethod(request.getMethod())
                    .setUri(request.getRequestURI())
                    .setRequestBody(wrapRequest.getPayload())
                    .setQueryString(request.getQueryString())
                    .setExecutionTime(end - start);
            visitRecordService.save(visitRecord);
        }

    }

    private void loginExpired(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.warn(request.getMethod() + request.getRequestURI());
        ApiResp apiResp = new ApiResp();
        apiResp.setError("login expired");
        String msg = objectMapper.writeValueAsString(apiResp);
        response.setStatus(403);
        response.getWriter().write(msg);
    }

    private boolean skipPathRequest(HttpServletRequest request, List<String> pathsToSkip) {
        List<RequestMatcher> m = pathsToSkip.map(AntPathRequestMatcher::new);
        return new OrRequestMatcher(m.toJavaList()).matches(request);
    }
}