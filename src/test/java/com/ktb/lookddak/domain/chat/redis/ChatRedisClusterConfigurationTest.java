package com.ktb.lookddak.domain.chat.redis;

import io.lettuce.core.cluster.SlotHash;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRedisClusterConfigurationTest {

    @Test
    void bindDeploymentClusterNodesAndRefreshSettings() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("deployment", Map.of(
                "REDIS_CLUSTER_NODES", "redis-0:6379,redis-1:6379,redis-2:6379",
                "REDIS_PASSWORD", "test-password"
        )));
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (String file : new String[]{"application-prod.yaml", "application.yaml"}) {
            loader.load(file, new FileSystemResource("src/main/resources/" + file))
                    .forEach(environment.getPropertySources()::addLast);
        }
        DataRedisProperties properties = Binder.get(environment)
                .bind("spring.data.redis", DataRedisProperties.class).get();

        assertThat(properties.getCluster().getNodes())
                .containsExactly("redis-0:6379", "redis-1:6379", "redis-2:6379");
        assertThat(properties.getPassword()).isEqualTo("test-password");
        assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(1));
        assertThat(properties.getLettuce().getCluster().getRefresh().getPeriod())
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getLettuce().getCluster().getRefresh().isDynamicRefreshSources()).isTrue();
    }

    @Test
    void allLuaKeysForEachMessageBelongToSameClusterSlot() {
        ChatRedisKeyFactory keys = new ChatRedisKeyFactory(new ChatRedisProperties(false, "lookddak:chat"));
        for (long messageId : new long[]{1, 4365, 999999}) {
            int slot = SlotHash.getSlot(keys.contentKey(messageId));
            assertThat(SlotHash.getSlot(keys.stateKey(messageId))).isEqualTo(slot);
            assertThat(SlotHash.getSlot(keys.appliedEventKey(messageId, "event-1"))).isEqualTo(slot);
            assertThat(SlotHash.getSlot(keys.appliedEventKey(messageId, "event-2"))).isEqualTo(slot);
        }
    }
}
