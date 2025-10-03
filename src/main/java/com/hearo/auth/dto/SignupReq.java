package com.hearo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupReq(
        @Email String email,
        @Size(min = 8, max = 64) String password,
        @NotBlank String nickname
) {}
