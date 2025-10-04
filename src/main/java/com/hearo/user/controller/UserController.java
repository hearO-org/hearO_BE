package com.hearo.user.controller;

import com.hearo.user.domain.User;
import com.hearo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService users;

    @GetMapping("/me") // 테스트용 api
    public Map<String, Object> me(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User u = users.getById(userId);
        return Map.of("id", u.getId(), "email", u.getEmail(), "nickname", u.getNickname(),
                      "authType", u.getAuthType().name());
    }
}
