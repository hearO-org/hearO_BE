package com.hearo.member.service;

import com.hearo.global.exception.ApiException;
import com.hearo.global.response.ErrorStatus;
import com.hearo.member.domain.MemberProfile;
import com.hearo.member.dto.request.OnboardingReq;
import com.hearo.member.dto.response.MemberProfileRes;
import com.hearo.member.dto.request.UpdateMemberProfileReq;
import com.hearo.member.repository.MemberProfileRepository;
import com.hearo.user.domain.User;
import com.hearo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final UserRepository userRepository;
    private final MemberProfileRepository profileRepository;

    /**
     * 온보딩 최초 1회: 프로필 생성
     * - 이미 프로필이 있으면 에러
     */
    @Transactional
    public MemberProfileRes completeOnboarding(Long userId, OnboardingReq req) {

        // 1. 유저 존재 여부 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorStatus.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 2. 이미 프로필이 있으면 온보딩 1회 제한
        if (profileRepository.existsByUser_Id(userId)) {
            throw new ApiException(
                    ErrorStatus.DUPLICATE_RESOURCE,
                    "이미 온보딩 프로필이 생성된 사용자입니다."
            );
        }

        // 3. MemberProfile 엔티티 생성 및 저장
        MemberProfile profile = MemberProfile.create(
                user,
                req.nickname(),
                req.gender(),
                req.birthday(),
                req.interestKeywords()
        );
        profileRepository.save(profile);

        // 4. 응답 DTO 로 변환
        return toRes(profile);
    }

    /**
     * 내 프로필 조회 (이미 온보딩을 마친 사용자)
     */
    public MemberProfileRes getMyProfile(Long userId) {
        MemberProfile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(
                        ErrorStatus.RESOURCE_NOT_FOUND,
                        "온보딩 프로필이 존재하지 않습니다.")
                );
        return toRes(profile);
    }

    /**
     * 내 프로필 수정
     * - 이미 온보딩이 되어 있어야 수정 가능
     */
    @Transactional
    public MemberProfileRes updateMyProfile(Long userId, UpdateMemberProfileReq req) {

        // 1. 내 프로필이 존재하는지 확인 (없으면 예외)
        MemberProfile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(
                        ErrorStatus.RESOURCE_NOT_FOUND,
                        "온보딩 프로필이 존재하지 않아 수정할 수 없습니다.")
                );

        // 2. 엔티티의 도메인 메서드를 통해 값 변경
        profile.update(
                req.nickname(),
                req.gender(),
                req.birthday(),
                req.interestKeywords()
        );
        // @Transactional + JPA 더티체킹으로 save() 안 해도 자동 update

        // 3. 수정된 내용을 응답 DTO로 변환
        return toRes(profile);
    }

    /* ===== 내부 매핑 메서드 ===== */

    private MemberProfileRes toRes(MemberProfile profile) {
        User user = profile.getUser();
        return new MemberProfileRes(
                user.getId(),
                user.getEmail(),
                profile.getNickname(),
                profile.getGender(),
                profile.getBirthday(),
                profile.getInterestKeywords()
        );
    }
}
