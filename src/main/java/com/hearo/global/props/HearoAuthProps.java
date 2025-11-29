package com.hearo.global.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hearo.auth")
public class HearoAuthProps {

    /**
     * 카카오 OAuth2 로그인 성공 후 모바일 딥링크 리다이렉트 URI
     * 예: hearo://login
     */
    private String successRedirect = "hearo://login"; // 프론트 URI

    public String getSuccessRedirect() { return successRedirect; }
    public void setSuccessRedirect(String s) { this.successRedirect = s; }
}
