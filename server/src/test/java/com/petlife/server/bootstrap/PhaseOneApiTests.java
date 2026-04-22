package com.petlife.server.bootstrap;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.petlife.server.modules.family.persistence.FamilyPersistenceMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PhaseOneApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FamilyPersistenceMapper familyPersistenceMapper;

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
        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet.pet_name", is("Momo")))
            .andExpect(jsonPath("$.data.family_summary.family_name", is("Momo的家庭")));
    }

    @Test
    void shouldCreatePet() throws Exception {
        mockMvc.perform(post("/api/v1/pets")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
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
        String authorizationHeader = authorizationHeader();
        String petId = createPet(authorizationHeader, "Dodo");

        mockMvc.perform(patch("/api/v1/me/settings/current-pet")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": "%s"
                    }
                    """.formatted(petId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", is(petId)));
    }

    @Test
    void shouldReturnFamilyDetail() throws Exception {
        mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.current_user_role", is("owner")))
            .andExpect(jsonPath("$.data.members[0].role", is("owner")))
            .andExpect(jsonPath("$.data.shared_pets[0].pet_name", is("Momo")));
    }

    @Test
    void shouldReturnExistingFamilyWhenInitializeFamilyCalled() throws Exception {
        mockMvc.perform(post("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.current_user_role", is("owner")));
    }

    @Test
    void shouldCreateFamilyInvitation() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(post("/api/v1/family/invitations")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "invitee_mobile": "13900000000",
                      "role": "member",
                      "shared_pet_ids": ["%s"]
                    }
                    """.formatted(petId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role", is("member")))
            .andExpect(jsonPath("$.data.shared_pet_ids[0]", is(petId)))
            .andExpect(jsonPath("$.data.status", is("pending")))
            .andExpect(jsonPath("$.data.invite_code").exists());
    }

    @Test
    void shouldPreviewAndAcceptFamilyInvitation() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader();
        String inviteeAuthorizationHeader = authorizationHeader("13900000000");
        String petId = currentPetId(ownerAuthorizationHeader);
        String inviteCode = createFamilyInvitation(ownerAuthorizationHeader, "13900000000", petId);

        mockMvc.perform(get("/api/v1/family/invitations/%s".formatted(inviteCode))
                .header(HttpHeaders.AUTHORIZATION, inviteeAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.role", is("member")))
            .andExpect(jsonPath("$.data.shared_pets[0].pet_id", is(petId)));

        mockMvc.perform(post("/api/v1/family/invitations/%s/accept".formatted(inviteCode))
                .header(HttpHeaders.AUTHORIZATION, inviteeAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.current_user_role", is("member")))
            .andExpect(jsonPath("$.data.shared_pets[0].pet_id", is(petId)));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, inviteeAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.family_summary.family_name", is("Momo的家庭")))
            .andExpect(jsonPath("$.data.current_pet_id", is(petId)));
    }

    @Test
    void shouldRejectFamilyInvitation() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader();
        String inviteeAuthorizationHeader = authorizationHeader("13700000000");
        String petId = currentPetId(ownerAuthorizationHeader);
        String inviteCode = createFamilyInvitation(ownerAuthorizationHeader, "13700000000", petId);

        mockMvc.perform(post("/api/v1/family/invitations/%s/reject".formatted(inviteCode))
                .header(HttpHeaders.AUTHORIZATION, inviteeAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("rejected")));

        mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, ownerAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.member_count", is(1)));
    }

    @Test
    void shouldUpdateFamilyMemberRole() throws Exception {
        String authorizationHeader = authorizationHeader();
        String memberId = joinFamilyMember(authorizationHeader, "13900000000", "member");

        mockMvc.perform(patch("/api/v1/family/members/%s/role".formatted(memberId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "role": "admin"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.member_id", is(memberId)))
            .andExpect(jsonPath("$.data.role", is("admin")));
    }

    @Test
    void shouldRemoveFamilyMember() throws Exception {
        String authorizationHeader = authorizationHeader();
        String memberId = joinFamilyMember(authorizationHeader, "13700000000", "member");

        mockMvc.perform(delete("/api/v1/family/members/%s".formatted(memberId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.member_count", is(1)));
    }

    @Test
    void shouldCreateHealthRecord() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
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
            .andExpect(jsonPath("$.data.title", is("耳螨滴药")))
            .andExpect(jsonPath("$.data.value", is("2")))
            .andExpect(jsonPath("$.data.unit", is("drops")));
    }

    @Test
    void shouldGetUpdateAndDeleteHealthRecord() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String healthRecordId = createHealthRecord(authorizationHeader, petId, "体重复查");

        mockMvc.perform(get("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.health_record_id", is(healthRecordId)))
            .andExpect(jsonPath("$.data.title", is("体重复查")));

        mockMvc.perform(patch("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "examination",
                      "title": "年度体检复查",
                      "value": "4.8",
                      "unit": "kg",
                      "occurred_at": "2026-04-19T10:30:00+08:00",
                      "notes": "精神状态稳定"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title", is("年度体检复查")))
            .andExpect(jsonPath("$.data.value", is("4.8")))
            .andExpect(jsonPath("$.data.unit", is("kg")));

        mockMvc.perform(delete("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.health_record_id == '%s')]".formatted(healthRecordId)).isEmpty());
    }

    @Test
    void shouldCompleteReminder() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String reminderId = createReminder(authorizationHeader, petId);

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/complete".formatted(petId, reminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("completed")))
            .andExpect(jsonPath("$.data.completed_at").exists());
    }

    @Test
    void shouldSkipReminder() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String reminderId = createReminder(authorizationHeader, petId);

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/skip".formatted(petId, reminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("skipped")))
            .andExpect(jsonPath("$.data.completed_at").exists());
    }

    @Test
    void shouldCreateNextReminderAfterCompletingCycleReminder() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime originalDueAt = OffsetDateTime.now().plusDays(7).withSecond(0).withNano(0);
        CreatedReminderFixture createdReminder = createCycleReminder(
            authorizationHeader,
            petId,
            "驱虫周期提醒",
            1,
            "month",
            originalDueAt
        );

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/complete".formatted(petId, createdReminder.reminderId()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("completed")))
            .andExpect(jsonPath("$.data.reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data.cycle_value", is(1)))
            .andExpect(jsonPath("$.data.cycle_unit", is("month")));

        MvcResult reminderListResult = mockMvc.perform(get("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(2)))
            .andExpect(jsonPath("$.data[0].status", is("pending")))
            .andExpect(jsonPath("$.data[0].title", is("驱虫周期提醒")))
            .andExpect(jsonPath("$.data[0].reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data[0].cycle_value", is(1)))
            .andExpect(jsonPath("$.data[0].cycle_unit", is("month")))
            .andExpect(jsonPath("$.data[1].status", is("completed")))
            .andReturn();

        String nextDueAtValue = JsonPath.read(reminderListResult.getResponse().getContentAsString(), "$.data[0].due_at");
        assertEquals(originalDueAt.plusMonths(1).toInstant(), OffsetDateTime.parse(nextDueAtValue).toInstant());
    }

    @Test
    void shouldCreateNextReminderAfterSkippingCycleReminder() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime originalDueAt = OffsetDateTime.now().plusDays(5).withSecond(0).withNano(0);
        CreatedReminderFixture createdReminder = createCycleReminder(
            authorizationHeader,
            petId,
            "复查周期提醒",
            2,
            "week",
            originalDueAt
        );

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/skip".formatted(petId, createdReminder.reminderId()))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("skipped")))
            .andExpect(jsonPath("$.data.reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data.cycle_value", is(2)))
            .andExpect(jsonPath("$.data.cycle_unit", is("week")));

        MvcResult reminderListResult = mockMvc.perform(get("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(2)))
            .andExpect(jsonPath("$.data[0].status", is("pending")))
            .andExpect(jsonPath("$.data[0].title", is("复查周期提醒")))
            .andExpect(jsonPath("$.data[0].reminder_mode", is("cycle")))
            .andExpect(jsonPath("$.data[0].cycle_value", is(2)))
            .andExpect(jsonPath("$.data[0].cycle_unit", is("week")))
            .andExpect(jsonPath("$.data[1].status", is("skipped")))
            .andReturn();

        String nextDueAtValue = JsonPath.read(reminderListResult.getResponse().getContentAsString(), "$.data[0].due_at");
        assertEquals(originalDueAt.plusWeeks(2).toInstant(), OffsetDateTime.parse(nextDueAtValue).toInstant());
    }

    @Test
    void shouldCreateDailyLog() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
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
    void shouldGetUpdateAndDeleteDailyLog() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。");

        mockMvc.perform(get("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.daily_log_id", is(dailyLogId)))
            .andExpect(jsonPath("$.data.content", is("今天第一次主动跳上窗台晒太阳。")));

        mockMvc.perform(patch("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天学会了在门口等人回家。",
                      "tags": ["等待", "互动"],
                      "visibility": "family",
                      "happened_at": "2026-04-18T19:30:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", is("今天学会了在门口等人回家。")))
            .andExpect(jsonPath("$.data.tags[0]", is("等待")))
            .andExpect(jsonPath("$.data.visibility", is("family")));

        mockMvc.perform(delete("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.daily_log_id == '%s')]".formatted(dailyLogId)).isEmpty());
    }

    @Test
    void shouldReturnTimelineEventsFromHealthAndDailyLogs() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String healthRecordId = createHealthRecord(authorizationHeader, petId, "体重复查");
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。");

        mockMvc.perform(get("/api/v1/pets/%s/timeline".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(2)))
            .andExpect(jsonPath("$.data[0].event_type", is("daily_log")))
            .andExpect(jsonPath("$.data[0].source_type", is("daily_log")))
            .andExpect(jsonPath("$.data[0].source_id", is(dailyLogId)))
            .andExpect(jsonPath("$.data[1].event_type", is("health")))
            .andExpect(jsonPath("$.data[1].source_type", is("health_record")))
            .andExpect(jsonPath("$.data[1].source_id", is(healthRecordId)));

        mockMvc.perform(get("/api/v1/pets/%s/timeline?event_type=health".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].event_type", is("health")))
            .andExpect(jsonPath("$.data[0].title", is("体重复查")))
            .andExpect(jsonPath("$.data[0].summary", is("4.3 kg · 饮食正常")));
    }

    @Test
    void shouldSyncTimelineEventsAfterUpdatingAndDeletingSourceRecords() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String healthRecordId = createHealthRecord(authorizationHeader, petId, "体重复查");
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。");

        mockMvc.perform(patch("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "examination",
                      "title": "年度体检复查",
                      "value": "4.8",
                      "unit": "kg",
                      "occurred_at": "2026-04-19T10:30:00+08:00",
                      "notes": "精神状态稳定"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天学会了在门口等人回家。",
                      "tags": ["等待", "互动"],
                      "visibility": "family",
                      "happened_at": "2026-04-18T19:30:00+08:00"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pets/%s/timeline?event_type=health".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title", is("年度体检复查")))
            .andExpect(jsonPath("$.data[0].summary", is("4.8 kg · 精神状态稳定")));

        mockMvc.perform(get("/api/v1/pets/%s/timeline?event_type=daily_log".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title", is("今天学会了在门口等人回家。")))
            .andExpect(jsonPath("$.data[0].summary", is("今天学会了在门口等人回家。")));

        mockMvc.perform(delete("/api/v1/pets/%s/health-records/%s".formatted(petId, healthRecordId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pets/%s/timeline".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturnAggregatedPetSummary() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createReminder(authorizationHeader, petId);
        createHealthRecord(authorizationHeader, petId, "体重复查");
        createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。");

        mockMvc.perform(get("/api/v1/pets/%s/summary".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.today_todo_count", is(1)))
            .andExpect(jsonPath("$.data.recent_health_records[0]", is("体重复查")))
            .andExpect(jsonPath("$.data.recent_daily_logs[0]").exists());
    }

    @Test
    void shouldRejectProtectedApiWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    private String authorizationHeader() throws Exception {
        return authorizationHeader("13800000000");
    }

    private String authorizationHeader(String mobile) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login/sms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mobile": "%s",
                      "code": "123456"
                    }
                    """.formatted(mobile)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = JsonPath.read(responseBody, "$.data.access_token");
        return "Bearer " + accessToken;
    }

    private String currentUserId(String authorizationHeader) throws Exception {
        MvcResult currentUserResult = mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(currentUserResult.getResponse().getContentAsString(), "$.data.user.user_id");
    }

    private String currentFamilyId(String authorizationHeader) throws Exception {
        MvcResult familyResult = mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(familyResult.getResponse().getContentAsString(), "$.data.family_id");
    }

    private String currentPetId(String authorizationHeader) throws Exception {
        MvcResult currentUserResult = mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(currentUserResult.getResponse().getContentAsString(), "$.data.current_pet_id");
    }

    private String createPet(String authorizationHeader, String petName) throws Exception {
        MvcResult createPetResult = mockMvc.perform(post("/api/v1/pets")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_name": "%s",
                      "pet_type": "dog",
                      "breed": "Corgi",
                      "gender": "male",
                      "birthday": "2024-03-08",
                      "adopt_date": "2024-06-18",
                      "neuter_status": "pending",
                      "avatar_asset_id": "asset_1001"
                    }
                    """.formatted(petName)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createPetResult.getResponse().getContentAsString(), "$.data.pet_id");
    }

    private String createHealthRecord(String authorizationHeader, String petId, String title) throws Exception {
        MvcResult createHealthRecordResult = mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "weight",
                      "title": "%s",
                      "value": "4.3",
                      "unit": "kg",
                      "occurred_at": "2026-04-17T08:30:00+08:00",
                      "notes": "饮食正常"
                    }
                    """.formatted(title)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createHealthRecordResult.getResponse().getContentAsString(), "$.data.health_record_id");
    }

    private String createReminder(String authorizationHeader, String petId) throws Exception {
        MvcResult createReminderResult = mockMvc.perform(post("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reminder_type": "deworming",
                      "title": "体内驱虫提醒",
                      "due_at": "2026-04-18T09:00:00+08:00",
                      "notes": "饭后执行"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createReminderResult.getResponse().getContentAsString(), "$.data.reminder_id");
    }

    private CreatedReminderFixture createCycleReminder(
        String authorizationHeader,
        String petId,
        String title,
        int cycleValue,
        String cycleUnit,
        OffsetDateTime dueAt
    ) throws Exception {
        MvcResult createReminderResult = mockMvc.perform(post("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reminder_type": "deworming",
                      "title": "%s",
                      "reminder_mode": "cycle",
                      "cycle_value": %s,
                      "cycle_unit": "%s",
                      "due_at": "%s",
                      "notes": "周期提醒测试"
                    }
                    """.formatted(title, cycleValue, cycleUnit, dueAt)))
            .andExpect(status().isOk())
            .andReturn();

        String responseBody = createReminderResult.getResponse().getContentAsString();
        return new CreatedReminderFixture(
            JsonPath.read(responseBody, "$.data.reminder_id"),
            JsonPath.read(responseBody, "$.data.due_at")
        );
    }

    private String createDailyLog(String authorizationHeader, String petId, String content) throws Exception {
        MvcResult createDailyLogResult = mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "%s",
                      "tags": ["晒太阳", "成长"],
                      "visibility": "public",
                      "happened_at": "2026-04-17T10:00:00+08:00"
                    }
                    """.formatted(content)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createDailyLogResult.getResponse().getContentAsString(), "$.data.daily_log_id");
    }

    private String createFamilyInvitation(String authorizationHeader, String mobile, String petId) throws Exception {
        MvcResult invitationResult = mockMvc.perform(post("/api/v1/family/invitations")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "invitee_mobile": "%s",
                      "role": "member",
                      "shared_pet_ids": ["%s"]
                    }
                    """.formatted(mobile, petId)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(invitationResult.getResponse().getContentAsString(), "$.data.invite_code");
    }

    @SuppressWarnings("unchecked")
    private String joinFamilyMember(String ownerAuthorizationHeader, String mobile, String role) throws Exception {
        String memberAuthorizationHeader = authorizationHeader(mobile);
        String memberUserId = currentUserId(memberAuthorizationHeader);
        String familyId = currentFamilyId(ownerAuthorizationHeader);
        familyPersistenceMapper.insertFamilyMember(Long.valueOf(familyId), Long.valueOf(memberUserId), role);

        MvcResult familyResult = mockMvc.perform(get("/api/v1/family")
                .header(HttpHeaders.AUTHORIZATION, ownerAuthorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        List<String> memberIds = JsonPath.read(
            familyResult.getResponse().getContentAsString(),
            "$.data.members[?(@.user_id == '%s')].member_id".formatted(memberUserId)
        );
        return memberIds.get(0);
    }

    private record CreatedReminderFixture(
        String reminderId,
        String dueAt
    ) {
    }
}
