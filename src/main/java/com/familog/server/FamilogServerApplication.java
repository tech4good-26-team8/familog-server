package com.familog.server;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FamilogServerApplication {

    public static void main(String[] args) {
        // 전 구간 KST 고정 (04 컨벤션 §3) — LocalDate/LocalDateTime.now()가 호스트 타임존을 타지 않도록
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(FamilogServerApplication.class, args);
    }

}
