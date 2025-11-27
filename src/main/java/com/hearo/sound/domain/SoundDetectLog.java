package com.hearo.sound.domain;

import com.hearo.global.entity.BaseTimeEntity;
import com.hearo.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sound_detect_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SoundDetectLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 로그인 유저 (로그인 필수 서비스이므로 nullable 아님)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String filename;

    @Column(name = "file_size")
    private Long fileSize; // bytes 기준

    @Column(length = 100)
    private String label;  // 예: siren, car_horn 등

    private Double confidence; // max prob

    private Boolean alert; // 위험 판단 여부

    /**
     * 확률 분포를 그냥 문자열로 스냅샷 (ex: {siren=0.9, car_horn=0.1})
     * 나중에 검색/통계용이면 JSON으로 바꾸고 Converter 써도 됨
     */
    @Lob
    @Column(name = "probs_snapshot")
    private String probsSnapshot;

    /**
     * 호출이 성공했는지 여부 (AI 응답까지 정상)
     */
    private Boolean success;

    /**
     * 실패 시 에러 메시지 간단 저장
     */
    @Column(length = 500)
    private String errorMessage;

    /**
     * 전체 처리 소요 시간(ms)
     */
    @Column(name = "elapsed_ms")
    private Long elapsedMs;

    // ------ 편의 팩토리 메서드 ------ //

    public static SoundDetectLog successLog(
            User user,
            String filename,
            Long fileSize,
            String label,
            Double confidence,
            Boolean alert,
            String probsSnapshot,
            Long elapsedMs
    ) {
        return SoundDetectLog.builder()
                .user(user)
                .filename(filename)
                .fileSize(fileSize)
                .label(label)
                .confidence(confidence)
                .alert(alert)
                .probsSnapshot(probsSnapshot)
                .success(true)
                .errorMessage(null)
                .elapsedMs(elapsedMs)
                .build();
    }

    public static SoundDetectLog failLog(
            User user,
            String filename,
            Long fileSize,
            String errorMessage,
            Long elapsedMs
    ) {
        return SoundDetectLog.builder()
                .user(user)
                .filename(filename)
                .fileSize(fileSize)
                .label(null)
                .confidence(0.0)
                .alert(false)
                .probsSnapshot(null)
                .success(false)
                .errorMessage(errorMessage)
                .elapsedMs(elapsedMs)
                .build();
    }
}
