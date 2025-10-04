package com.hearo.user.service;

import com.hearo.global.exception.ApiException;
import com.hearo.global.response.ErrorStatus;
import com.hearo.user.domain.User;
import com.hearo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Transactional
    public User createLocalUser(String email, String rawPassword, String nickname) {
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorStatus.DUPLICATE_RESOURCE, "이미 사용 중인 이메일입니다.");
        }
        return users.save(User.createLocal(email, encoder.encode(rawPassword), nickname));
    }

    @Transactional
    public User upsertKakaoUser(String kakaoId, String emailOrNull, String nickname) {
        return users.findByKakaoId(kakaoId).orElseGet(() ->
                users.save(User.createKakao(kakaoId, emailOrNull, nickname)));
    }

    public User getById(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(ErrorStatus.USER_NOT_FOUND));
    }
}
