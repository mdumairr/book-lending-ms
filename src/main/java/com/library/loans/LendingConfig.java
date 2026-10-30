package com.library.loans;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(LendingProperties.class)
class LendingConfig {

    @Bean
    Clock clock(LendingProperties properties) {
        return Clock.system(ZoneId.of(properties.zoneId()));
    }
}
