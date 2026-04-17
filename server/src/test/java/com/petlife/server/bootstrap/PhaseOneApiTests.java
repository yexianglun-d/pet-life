package com.petlife.server.bootstrap;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petlife.server.bootstrap.devsupport.BootstrapMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PhaseOneApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BootstrapMemoryStore bootstrapMemoryStore;

    @BeforeEach
    void resetBootstrapStore() {
        bootstrapMemoryStore.reset();
    }

    @Test
    void shouldLoginBySms() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "13800000000",
                      "code": "123456"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")))
            .andExpect(jsonPath("$.data.access_token").exists())
            .andExpect(jsonPath("$.data.user.nickname", is("Momo")));
    }

    @Test
    void shouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", is("10001")))
            .andExpect(jsonPath("$.data.family_summary.family_name", is("Momo Family")));
    }

    @Test
    void shouldCreatePet() throws Exception {
        mockMvc.perform(post("/api/v1/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_name": "Nana",
                      "pet_type": "cat",
                      "breed": "Ragdoll",
                      "gender": "female",
                      "birthday": "2024-03-08",
                      "adopt_date": "2024-06-18",
                      "neuter_status": "pending",
                      "avatar_asset_id": "asset_1001"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pet_name", is("Nana")))
            .andExpect(jsonPath("$.data.pet_type", is("cat")));
    }

    @Test
    void shouldUpdateCurrentPet() throws Exception {
        mockMvc.perform(patch("/api/v1/me/settings/current-pet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": "10002"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", is("10002")));
    }

    @Test
    void shouldCreateHealthRecord() throws Exception {
        mockMvc.perform(post("/api/v1/pets/10001/health-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "medication",
                      "title": "耳螨滴药",
                      "value": "2",
                      "unit": "drops",
                      "occurred_at": "2026-04-17T08:30:00+08:00",
                      "notes": "晚饭后执行"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.record_type", is("medication")))
            .andExpect(jsonPath("$.data.title", is("耳螨滴药")));
    }

    @Test
    void shouldCompleteReminder() throws Exception {
        mockMvc.perform(patch("/api/v1/pets/10001/reminders/40001/complete"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("completed")))
            .andExpect(jsonPath("$.data.completed_at").exists());
    }

    @Test
    void shouldCreateDailyLog() throws Exception {
        mockMvc.perform(post("/api/v1/pets/10001/daily-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天第一次主动跳上窗台晒太阳。",
                      "tags": ["晒太阳", "成长"],
                      "visibility": "public",
                      "happened_at": "2026-04-17T10:00:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility", is("public")))
            .andExpect(jsonPath("$.data.tags[0]", is("晒太阳")));
    }

    @Test
    void shouldReturnAggregatedPetSummary() throws Exception {
        mockMvc.perform(get("/api/v1/pets/10001/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.today_todo_count", is(2)))
            .andExpect(jsonPath("$.data.recent_health_records[0]", is("体重复查")))
            .andExpect(jsonPath("$.data.recent_daily_logs[0]").exists());
    }
}
