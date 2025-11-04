package com.hearo.auth.handler;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {
    public OAuth2FailureHandler() { super("/auth/kakao/failure"); }
}
