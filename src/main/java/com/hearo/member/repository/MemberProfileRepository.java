package com.hearo.member.repository;

import com.hearo.member.domain.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    /**
     * user_id 기준으로 프로필 조회
     */
    Optional<MemberProfile> findByUser_Id(Long userId);

    /**
     * 프로필 존재 여부만 빠르게 확인
     */
    boolean existsByUser_Id(Long userId);
}
