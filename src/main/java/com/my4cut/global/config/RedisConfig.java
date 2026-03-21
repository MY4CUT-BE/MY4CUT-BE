package com.my4cut.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/*
 * Redis 문자열 기반 작업을 위한 설정 클래스
 *
 * - 이메일 인증에서는 인증코드, 재전송 제한 여부, 실패 횟수, 인증 완료 여부를
 *   모두 문자열 형태로 저장한다.
 * - 따라서 일반 RedisTemplate보다 StringRedisTemplate을 사용하는 것이 단순하고 명확하다.
 */
@Configuration
public class RedisConfig {

    /*
     * StringRedisTemplate Bean 등록
     *
     * - key: String
     * - value: String
     * 형태로 Redis에 저장/조회할 때 사용한다.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}