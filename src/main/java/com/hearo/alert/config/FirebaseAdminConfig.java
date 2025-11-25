//package com.hearo.alert.config;
//
//import com.google.auth.oauth2.GoogleCredentials;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.FirebaseOptions;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.FileInputStream;
//
//@Configuration
//@RequiredArgsConstructor
//public class FirebaseAdminConfig implements InitializingBean {
//
//    @Value("${firebase.service-account-file}") // 아직 없음 -> 앱 패키지 이름
//    private String serviceAccountPath;
//
//    @Override
//    public void afterPropertiesSet() throws Exception {
//        if (!FirebaseApp.getApps().isEmpty()) {
//            return; // 이미 초기화 되어 있으면 skip
//        }
//
//        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
//            FirebaseOptions options = FirebaseOptions.builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                    .build();
//            FirebaseApp.initializeApp(options);
//        }
//    }
//}
