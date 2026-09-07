package com.nsauto.leaderlock;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.nsauto.leaderlock.redisson.RedissonLeaderLockProvider;

import io.micrometer.core.instrument.MeterRegistry;

@AutoConfiguration
@EnableConfigurationProperties(LeaderLockProperties.class)
public class LeaderLockAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(LeaderLockProvider.class)
    public LeaderLockProvider leaderLockProvider(LeaderLockProperties properties) {
        return RedissonLeaderLockProvider.create(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LeaderElectionService leaderElectionService(LeaderLockProvider provider,
            LeaderLockProperties properties, MeterRegistry meterRegistry,
            List<LeaderElectionListener> listeners) {
        return new LeaderElectionService(provider, properties, meterRegistry, listeners);
    }

    @Bean(name = "leadership")
    @ConditionalOnMissingBean
    public LeadershipHealthIndicator leadershipHealthIndicator(LeaderElectionService service) {
        return new LeadershipHealthIndicator(service);
    }

    @Bean(name = "lockBackend")
    @ConditionalOnMissingBean
    public RedisConnectionHealthIndicator redisConnectionHealthIndicator(LeaderLockProvider provider) {
        return new RedisConnectionHealthIndicator(provider);
    }
}