package com.hearo.global.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hearo.auth")
public class HearoAuthProps {
    /** 추후 이 api는 프론트와 상의 후 변경예정 hearo://auth/callback */
    private String successRedirect = "hearo://auth/callback";
    public String getSuccessRedirect() { return successRedirect; }
    public void setSuccessRedirect(String s) { this.successRedirect = s; }
}
