package com.ktb.lookddak.global.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,prometheus",
        "management.metrics.distribution.percentiles-histogram.http.server.requests=true",
        "management.metrics.distribution.minimum-expected-value.http.server.requests=5ms",
        "management.metrics.distribution.maximum-expected-value.http.server.requests=60s",
        "management.metrics.distribution.slo.http.server.requests=500ms,1s,3s"
})
@AutoConfigureMockMvc
class ActuatorPrometheusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebEndpointsSupplier webEndpointsSupplier;

    @Test
    @DisplayName("인증 없이 Prometheus 형식의 기본 지표를 조회한다")
    void getPrometheusMetricsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("process_uptime_seconds")))
                .andExpect(content().string(containsString("http_server_requests_seconds_count")))
                .andExpect(content().string(containsString("http_server_requests_seconds_sum")))
                .andExpect(content().string(containsString("http_server_requests_seconds_bucket")))
                .andExpect(content().string(containsString("hikaricp_connections_active")))
                .andExpect(content().string(containsString("executor_active_threads")));
    }

    @Test
    @DisplayName("Health와 Prometheus 외의 Actuator Endpoint는 노출하지 않는다")
    void exposesOnlyConfiguredActuatorEndpoints() {
        var exposedEndpointIds = webEndpointsSupplier.getEndpoints().stream()
                .map(endpoint -> endpoint.getEndpointId().toString())
                .toList();

        assertThat(exposedEndpointIds)
                .containsExactlyInAnyOrder("health", "prometheus");
    }
}
