package com.hearo.sound.repository;

import com.hearo.sound.domain.SoundDetectLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoundDetectLogRepository extends JpaRepository<SoundDetectLog, Long> {

    // 유저별 최근 로그 조회 (user.id 기준)
    Page<SoundDetectLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
