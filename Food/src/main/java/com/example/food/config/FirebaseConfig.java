package com.example.food.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Firebase Admin SDK Configuration
 *
 * Hỗ trợ 2 cách khởi tạo:
 * 1. Dùng Service Account Key file (cho development)
 * 2. Dùng Default Credentials (cho production - nếu deploy lên GCP/Cloud Run)
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    private static final String FIREBASE_DB_URL = "https://foodapp-4da5f-default-rtdb.asia-southeast1.firebasedatabase.app";

    // Tên file service account key - có thể là tên mặc định hoặc tên thực tế từ Firebase
    // Code sẽ tìm theo thứ tự, file nào tìm thấy trước sẽ được dùng
    private static final String[] SERVICE_ACCOUNT_KEY_PATHS = {
            "foodapp-4da5f-firebase-adminsdk-fbsvc-784b66fa15.json",  // Tên thực tế từ Firebase (ưu tiên)
            "firebase-service-account-key.json"  // Tên mặc định (backup)
    };

    @Bean
    public FirebaseDatabase firebaseDatabase() {
        try {
            // Kiểm tra xem Firebase đã được khởi tạo chưa
            FirebaseApp firebaseApp;
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("🔥 Initializing Firebase Admin SDK...");

                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setDatabaseUrl(FIREBASE_DB_URL);

                // Thử dùng Service Account Key file trước
                GoogleCredentials credentials = getCredentials();
                if (credentials != null) {
                    optionsBuilder.setCredentials(credentials);
                    log.info("✅ Using Service Account Key file from resources");
                } else {
                    // Nếu không có service account key, dùng default credentials
                    // (hoạt động nếu deploy lên GCP/Cloud Run)
                    optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
                    log.info("✅ Using Default Application Credentials (GCP)");
                }

                firebaseApp = FirebaseApp.initializeApp(optionsBuilder.build());
                log.info("✅ Firebase Admin SDK initialized successfully");
            } else {
                firebaseApp = FirebaseApp.getInstance();
                log.info("🔥 Firebase Admin SDK already initialized, reusing existing instance");
            }

            // Lấy FirebaseDatabase instance
            FirebaseDatabase database = FirebaseDatabase.getInstance(firebaseApp);
            log.info("✅ FirebaseDatabase instance created with URL: {}", FIREBASE_DB_URL);

            return database;
        } catch (Exception e) {
            log.error("❌ Error initializing Firebase Admin SDK", e);
            throw new RuntimeException("Failed to initialize Firebase Admin SDK", e);
        }
    }

    /**
     * Lấy GoogleCredentials từ Service Account Key file
     * Hỗ trợ tìm file từ:
     * 1. Classpath (src/main/resources/) - Ưu tiên
     * 2. Root directory của project
     * 3. Absolute path
     */
    private GoogleCredentials getCredentials() {
        // Thử tìm file theo danh sách paths
        for (String path : SERVICE_ACCOUNT_KEY_PATHS) {
            try {
                InputStream serviceAccountStream = null;

                // Thử 1: Tìm trong classpath (src/main/resources/) - Ưu tiên cao nhất
                serviceAccountStream = getClass().getClassLoader()
                        .getResourceAsStream(path);

                if (serviceAccountStream == null) {
                    // Thử 2: Tìm trong root directory của project
                    if (Files.exists(Paths.get(path))) {
                        serviceAccountStream = new FileInputStream(path);
                        log.info("📁 Found service account key in project root: {}", path);
                    } else {
                        // Thử 3: Tìm trong thư mục hiện tại
                        String currentDir = System.getProperty("user.dir");
                        String fullPath = currentDir + "/" + path;
                        if (Files.exists(Paths.get(fullPath))) {
                            serviceAccountStream = new FileInputStream(fullPath);
                            log.info("📁 Found service account key: {}", fullPath);
                        }
                    }
                } else {
                    log.info("✅ Found service account key in classpath (resources): {}", path);
                }

                if (serviceAccountStream != null) {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
                    serviceAccountStream.close();
                    log.info("✅ Successfully loaded Service Account Key from: {}", path);
                    return credentials;
                }
            } catch (IOException e) {
                log.debug("⚠️ Error loading Service Account Key file '{}': {}", path, e.getMessage());
                // Tiếp tục thử file tiếp theo
                continue;
            } catch (Exception e) {
                log.debug("⚠️ Unexpected error with file '{}': {}", path, e.getMessage());
                continue;
            }
        }

        // Nếu không tìm thấy file nào
        log.warn("⚠️ Service Account Key file not found in any of the expected locations:");
        for (String path : SERVICE_ACCOUNT_KEY_PATHS) {
            log.warn("   - {}", path);
        }
        log.warn("⚠️ Will try default credentials (GCP)");
        return null;
    }
}

