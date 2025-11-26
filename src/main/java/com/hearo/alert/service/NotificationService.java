package com.hearo.alert.service;

import com.google.firebase.messaging.*;
import com.hearo.alert.domain.DeviceToken;
import com.hearo.alert.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * 위험 소리 감지 시 호출
     */
    public void sendDangerAlert(Long userId, String label, double confidence) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.info("No active device tokens for user {}", userId);
            return;
        }

        String title = "위험 소리 감지";
        String body = String.format("%s 소리가 감지되었습니다 (%.0f%%)", label, confidence * 100);

        for (DeviceToken token : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(
                                Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build()
                        )
                        .putData("label", label)
                        .putData("confidence", String.valueOf(confidence))
                        .putData("type", "SOUND_ALERT")
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Sent FCM to token={} response={}", token.getToken(), response);

            } catch (Exception e) {
                log.error("Failed to send FCM to token=" + token.getToken(), e);
                // 필요하다면, invalid token일 때 active=false 처리 등 추가 가능
            }
        }
    }
}
