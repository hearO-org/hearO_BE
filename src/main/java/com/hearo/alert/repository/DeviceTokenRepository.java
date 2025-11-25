package com.hearo.alert.repository;

import com.hearo.alert.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    List<DeviceToken> findByUserIdAndActiveTrue(Long userId);

    Optional<DeviceToken> findByToken(String token);
}
