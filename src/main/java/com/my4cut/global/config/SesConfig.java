package com.my4cut.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/*
 * AWS SES 클라이언트 설정 클래스
 *
 * - 이메일 인증 메일 발송에 사용할 SES 클라이언트를 Bean으로 등록한다.
 * - 리전은 실제 SES를 설정한 리전과 반드시 동일해야 한다.
 */
@Configuration
public class SesConfig {

    @Bean
    public SesV2Client sesV2Client() {
        return SesV2Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }
}