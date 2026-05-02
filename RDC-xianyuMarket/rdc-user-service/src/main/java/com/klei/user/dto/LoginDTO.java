package com.klei.user.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username;
    private String password;
    private String captcha;
    private String captchaUuid;
    private Boolean rememberMe;
}