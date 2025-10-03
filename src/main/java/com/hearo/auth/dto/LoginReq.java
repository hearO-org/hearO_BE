package com.hearo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginReq(
        @Email String email,
        @NotBlank String password
) {}
