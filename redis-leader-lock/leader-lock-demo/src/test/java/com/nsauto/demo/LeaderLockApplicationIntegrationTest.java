package com.nsauto.demo;

import com.nsauto.leaderlock.LeaderLock;
import com.nsauto.leaderlock.LeaderLockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@Import(LeaderLockApplicationIntegrationTest.FakeBackendConfiguration.class)
class LeaderLockApplicationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void exposesLeaderStatusThroughTheApplication() throws Exception {
        mockMvc.perform(get("/api/leader"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("STANDBY")));
    }

    @Test
    void exposesBackendHealthThroughTheApplication() throws Exception {
        mockMvc.perform(get("/api/health/backend"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fake")));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeBackendConfiguration {

        @Bean
        LeaderLockProvider leaderLockProvider() {
            return new LeaderLockProvider() {
                @Override
                public LeaderLock getLock(String key) {
                    return new LeaderLock() {
                        @Override
                        public boolean tryAcquire() {
                            return false;
                        }

                        @Override
                        public boolean isOwnedByCurrentThread() {
                            return false;
                        }

                        @Override
                        public void release() {
                        }
                    };
                }

                @Override
                public String backendName() {
                    return "fake";
                }
            };
        }
    }
}