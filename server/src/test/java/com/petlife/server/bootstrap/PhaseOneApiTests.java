package com.petlife.server.bootstrap;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
    void shouldRefreshAndLogoutSession() throws Exception {
        LoginTokensFixture loginTokens = loginTokens("13800000000");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refresh_token": "%s"
                    }
                    """.formatted(loginTokens.refreshToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.access_token").exists())
            .andExpect(jsonPath("$.data.refresh_token").exists())
            .andReturn();

        String refreshedResponseBody = refreshResult.getResponse().getContentAsString();
        String nextAccessToken = JsonPath.read(refreshedResponseBody, "$.data.access_token");
        String nextRefreshToken = JsonPath.read(refreshedResponseBody, "$.data.refresh_token");

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, loginTokens.authorizationHeader()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + nextAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet.pet_name", is("Momo")));

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refresh_token": "%s"
                    }
                    """.formatted(nextRefreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + nextAccessToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
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
    void shouldGetPetDetailAndUpdateExtendedPetFields() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(patch("/api/v1/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_name": "Momo",
                      "pet_type": "cat",
                      "breed": "British Shorthair",
                      "gender": "female",
                      "birthday": "2023-05-20",
                      "adopt_date": "2023-08-01",
                      "neuter_status": "completed",
                      "weight_kg": "4.8",
                      "allergy_notes": "对海鲜较敏感",
                      "medical_history": "2025 年做过牙结石清理"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.weight_kg", is("4.8")))
            .andExpect(jsonPath("$.data.allergy_notes", is("对海鲜较敏感")))
            .andExpect(jsonPath("$.data.medical_history", is("2025 年做过牙结石清理")))
            .andExpect(jsonPath("$.data.status", is("active")));

        mockMvc.perform(get("/api/v1/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.weight_kg", is("4.8")))
            .andExpect(jsonPath("$.data.allergy_notes", is("对海鲜较敏感")))
            .andExpect(jsonPath("$.data.medical_history", is("2025 年做过牙结石清理")));
    }

    @Test
    void shouldArchiveCurrentPetAndRebuildCurrentPetContext() throws Exception {
        String authorizationHeader = authorizationHeader();
        String fallbackPetId = currentPetId(authorizationHeader);
        String archivedPetId = createPet(authorizationHeader, "Dodo");

        mockMvc.perform(patch("/api/v1/me/settings/current-pet")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "pet_id": "%s"
                    }
                    """.formatted(archivedPetId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", is(archivedPetId)));

        mockMvc.perform(patch("/api/v1/pets/%s/archive".formatted(archivedPetId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "archive_status": "memorial"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", is(fallbackPetId)))
            .andExpect(jsonPath("$.data.current_pet.pet_name", is("Momo")));

        mockMvc.perform(get("/api/v1/pets")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.pet_id == '%s')]".formatted(archivedPetId)).isEmpty());
    }

    @Test
    void shouldDeleteLastPetAndReturnNoCurrentPetState() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);

        mockMvc.perform(delete("/api/v1/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is("OK")));

        mockMvc.perform(get("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.current_pet_id", nullValue()))
            .andExpect(jsonPath("$.data.current_pet", nullValue()));

        mockMvc.perform(get("/api/v1/pets")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldRejectDeletePetForMemberRole() throws Exception {
        String ownerAuthorizationHeader = authorizationHeader();
        String memberAuthorizationHeader = authorizationHeader("13700000000");
        String memberUserId = currentUserId(memberAuthorizationHeader);
        String familyId = currentFamilyId(ownerAuthorizationHeader);
        String petId = currentPetId(ownerAuthorizationHeader);

        familyPersistenceMapper.insertFamilyMember(Long.valueOf(familyId), Long.valueOf(memberUserId), "member");

        mockMvc.perform(delete("/api/v1/pets/%s".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, memberAuthorizationHeader))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code", is("PET_PERMISSION_DENIED")));
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
                      "sync_to_community": false,
                      "happened_at": "2026-04-17T10:00:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.visibility", is("public")))
            .andExpect(jsonPath("$.data.sync_to_community", is(false)))
            .andExpect(jsonPath("$.data.tags[0]", is("晒太阳")));
    }

    @Test
    void shouldSyncDailyLogToCommunityFeedAndDetail() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        mockMvc.perform(get("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sync_to_community", is(true)))
            .andExpect(jsonPath("$.data.community_post_id").exists());

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].source_daily_log_id", is(dailyLogId)))
            .andExpect(jsonPath("$.data[0].title", is("今天第一次主动跳上窗台晒太阳。")))
            .andExpect(jsonPath("$.data[0].author.nickname", is("Momo")))
            .andReturn();

        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");
        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.source_daily_log_id", is(dailyLogId)))
            .andExpect(jsonPath("$.data.pet.pet_name", is("Momo")))
            .andExpect(jsonPath("$.data.content", is("今天第一次主动跳上窗台晒太阳。")));
    }

    @Test
    void shouldUpdateAndWithdrawCommunityPostWhenDailyLogSyncChanges() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(patch("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天会主动叼玩具找人互动。",
                      "tags": ["互动", "成长"],
                      "visibility": "public",
                      "sync_to_community": true,
                      "happened_at": "2026-04-18T19:30:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sync_to_community", is(true)))
            .andExpect(jsonPath("$.data.community_post_id", is(postId)));

        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content", is("今天会主动叼玩具找人互动。")))
            .andExpect(jsonPath("$.data.title", is("今天会主动叼玩具找人互动。")));

        mockMvc.perform(patch("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "今天会主动叼玩具找人互动。",
                      "tags": ["互动", "成长"],
                      "visibility": "family",
                      "sync_to_community": false,
                      "happened_at": "2026-04-18T19:30:00+08:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sync_to_community", is(false)));

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')]".formatted(postId)).isEmpty());
    }

    @Test
    void shouldDeleteCommunityPostWhenDeletingSyncedDailyLog() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        String dailyLogId = createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(delete("/api/v1/pets/%s/daily-logs/%s".formatted(petId, dailyLogId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.post_id == '%s')]".formatted(postId)).isEmpty());
    }

    @Test
    void shouldCreateAndListCommunityComments() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(post("/api/v1/community/posts/%s/comments".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "这条观察很真实，能看出已经越来越放松了。"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.content", is("这条观察很真实，能看出已经越来越放松了。")))
            .andExpect(jsonPath("$.data.author.nickname", is("Momo")));

        mockMvc.perform(get("/api/v1/community/posts/%s/comments".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].content", is("这条观察很真实，能看出已经越来越放松了。")));

        mockMvc.perform(get("/api/v1/community/posts/%s".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.comment_count", is(1)));
    }

    @Test
    void shouldLikeAndUnlikeCommunityPost() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(post("/api/v1/community/posts/%s/like".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.liked", is(true)))
            .andExpect(jsonPath("$.data.like_count", is(1)));

        mockMvc.perform(delete("/api/v1/community/posts/%s/like".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.liked", is(false)))
            .andExpect(jsonPath("$.data.like_count", is(0)));
    }

    @Test
    void shouldFavoriteAndUnfavoriteCommunityPost() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        createDailyLog(authorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        mockMvc.perform(post("/api/v1/community/posts/%s/favorite".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.post_id", is(postId)))
            .andExpect(jsonPath("$.data.favorited", is(true)))
            .andExpect(jsonPath("$.data.favorite_count", is(1)));

        mockMvc.perform(delete("/api/v1/community/posts/%s/favorite".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.favorited", is(false)))
            .andExpect(jsonPath("$.data.favorite_count", is(0)));
    }

    @Test
    void shouldCreateAndReusePendingCommunityReport() throws Exception {
        String authorAuthorizationHeader = authorizationHeader();
        String reporterAuthorizationHeader = authorizationHeader("13900000000");
        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);

        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andReturn();
        String postId = JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");

        MvcResult firstReportResult = mockMvc.perform(post("/api/v1/community/posts/%s/report".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason_code": "spam",
                      "reason_detail": "连续出现重复引流内容"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.target_type", is("post")))
            .andExpect(jsonPath("$.data.target_id", is(postId)))
            .andExpect(jsonPath("$.data.reason_code", is("spam")))
            .andExpect(jsonPath("$.data.reason_detail", is("连续出现重复引流内容")))
            .andExpect(jsonPath("$.data.status", is("pending")))
            .andReturn();

        String reportId = JsonPath.read(firstReportResult.getResponse().getContentAsString(), "$.data.report_id");

        mockMvc.perform(post("/api/v1/community/posts/%s/report".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason_code": "spam",
                      "reason_detail": "再次提交同一条举报"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.report_id", is(reportId)))
            .andExpect(jsonPath("$.data.reason_detail", is("连续出现重复引流内容")))
            .andExpect(jsonPath("$.data.status", is("pending")));
    }

    @Test
    void shouldListModerationReports() throws Exception {
        String authorAuthorizationHeader = authorizationHeader();
        String reporterAuthorizationHeader = authorizationHeader("13900000000");
        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);
        String postId = currentCommunityPostId(reporterAuthorizationHeader);
        createCommunityPostReport(reporterAuthorizationHeader, postId, "harassment", "持续使用攻击性语言");

        mockMvc.perform(get("/api/v1/admin/moderation/reports")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].reporter_nickname", is("宠物家长")))
            .andExpect(jsonPath("$.data[0].reason_code", is("harassment")))
            .andExpect(jsonPath("$.data[0].post_title", is("今天第一次主动跳上窗台晒太阳。")))
            .andExpect(jsonPath("$.data[0].status", is("pending")));
    }

    @Test
    void shouldConfirmViolationAndHideReportedPost() throws Exception {
        String authorAuthorizationHeader = authorizationHeader();
        String reporterAuthorizationHeader = authorizationHeader("13900000000");
        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);
        String postId = currentCommunityPostId(reporterAuthorizationHeader);
        String reportId = createCommunityPostReport(reporterAuthorizationHeader, postId, "illegal", "包含违规售卖信息");

        mockMvc.perform(patch("/api/v1/admin/moderation/reports/%s".formatted(reportId))
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader)
                .header("X-Admin-Operator", "risk-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "confirm_violation"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("processed")))
            .andExpect(jsonPath("$.data.processed_by", is("risk-admin")))
            .andExpect(jsonPath("$.data.post_review_status", is("rejected")));

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldDismissReportAndKeepReportedPostVisible() throws Exception {
        String authorAuthorizationHeader = authorizationHeader();
        String reporterAuthorizationHeader = authorizationHeader("13900000000");
        String petId = currentPetId(authorAuthorizationHeader);
        createDailyLog(authorAuthorizationHeader, petId, "今天第一次主动跳上窗台晒太阳。", true);
        String postId = currentCommunityPostId(reporterAuthorizationHeader);
        String reportId = createCommunityPostReport(reporterAuthorizationHeader, postId, "spam", "误报测试");

        mockMvc.perform(patch("/api/v1/admin/moderation/reports/%s".formatted(reportId))
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader)
                .header("X-Admin-Operator", "content-admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "action": "dismiss_report"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", is("rejected")))
            .andExpect(jsonPath("$.data.processed_by", is("content-admin")))
            .andExpect(jsonPath("$.data.post_review_status", is("approved")));

        mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, reporterAuthorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()", is(1)))
            .andExpect(jsonPath("$.data[0].post_id", is(postId)));
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
                      "sync_to_community": false,
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
                      "sync_to_community": false,
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
    void shouldReturnWeeklyPetReport() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime now = OffsetDateTime.now().withSecond(0).withNano(0);

        createReminderAt(
            authorizationHeader,
            petId,
            "疫苗复查提醒",
            now.minusDays(1).withHour(9).withMinute(0)
        );
        String completedReminderId = createReminderAt(
            authorizationHeader,
            petId,
            "体内驱虫提醒",
            now.minusDays(2).withHour(10).withMinute(0)
        );
        String skippedReminderId = createReminderAt(
            authorizationHeader,
            petId,
            "复查观察提醒",
            now.minusDays(3).withHour(11).withMinute(0)
        );
        createWeightHealthRecordAt(
            authorizationHeader,
            petId,
            "本周体重记录",
            now.minusDays(2).withHour(8).withMinute(30),
            "4.5",
            "称重时精神状态稳定"
        );
        createMedicationHealthRecordAt(
            authorizationHeader,
            petId,
            "益生菌补充",
            now.minusDays(1).withHour(20).withMinute(0),
            "晚饭后补充"
        );
        createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            "快捷记录：今天完成了一次喂食。",
            List.of("快捷记录", "喂食"),
            false,
            now.minusDays(1).withHour(8).withMinute(0)
        );
        createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            "快捷记录：今天补记了一次饮水。",
            List.of("快捷记录", "饮水"),
            false,
            now.minusDays(1).withHour(14).withMinute(0)
        );
        createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            "快捷记录：今天补记了一次排便观察。",
            List.of("快捷记录", "排便"),
            true,
            now.minusDays(2).withHour(18).withMinute(0)
        );

        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/complete".formatted(petId, completedReminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/pets/%s/reminders/%s/skip".formatted(petId, skippedReminderId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/home/reports/weekly")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.report_type", is("weekly")))
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.pending_reminder_count", is(1)))
            .andExpect(jsonPath("$.data.completed_reminder_count", is(1)))
            .andExpect(jsonPath("$.data.skipped_reminder_count", is(1)))
            .andExpect(jsonPath("$.data.health_record_count", is(2)))
            .andExpect(jsonPath("$.data.daily_log_count", is(3)))
            .andExpect(jsonPath("$.data.community_sync_count", is(1)))
            .andExpect(jsonPath("$.data.feed_count", is(1)))
            .andExpect(jsonPath("$.data.water_count", is(1)))
            .andExpect(jsonPath("$.data.toilet_count", is(1)))
            .andExpect(jsonPath("$.data.weight_record_count", is(1)))
            .andExpect(jsonPath("$.data.medication_record_count", is(1)))
            .andExpect(jsonPath("$.data.highlights[0]").exists())
            .andExpect(jsonPath("$.data.recent_reminders[0].reminder_id").exists())
            .andExpect(jsonPath("$.data.recent_health_records[0].title", is("益生菌补充")))
            .andExpect(jsonPath("$.data.recent_daily_logs[0].tags[0]", is("快捷记录")));
    }

    @Test
    void shouldReturnMonthlyPetReport() throws Exception {
        String authorizationHeader = authorizationHeader();
        String petId = currentPetId(authorizationHeader);
        OffsetDateTime now = OffsetDateTime.now().withSecond(0).withNano(0);

        createReminderAt(
            authorizationHeader,
            petId,
            "月度体检提醒",
            now.minusDays(10).withHour(9).withMinute(0)
        );
        createWeightHealthRecordAt(
            authorizationHeader,
            petId,
            "月度体重记录",
            now.minusDays(12).withHour(8).withMinute(0),
            "4.8",
            "本月状态平稳"
        );
        createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            "这个月第一次主动在门口等人回家。",
            List.of("成长", "互动"),
            false,
            now.minusDays(9).withHour(19).withMinute(30)
        );

        mockMvc.perform(get("/api/v1/home/reports/monthly")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.report_type", is("monthly")))
            .andExpect(jsonPath("$.data.pet.pet_id", is(petId)))
            .andExpect(jsonPath("$.data.window_start").exists())
            .andExpect(jsonPath("$.data.window_end").exists())
            .andExpect(jsonPath("$.data.health_record_count", is(1)))
            .andExpect(jsonPath("$.data.daily_log_count", is(1)))
            .andExpect(jsonPath("$.data.recent_daily_logs[0].content", is("这个月第一次主动在门口等人回家。")));
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
        return loginTokens(mobile).authorizationHeader();
    }

    private LoginTokensFixture loginTokens(String mobile) throws Exception {
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
        String refreshToken = JsonPath.read(responseBody, "$.data.refresh_token");
        return new LoginTokensFixture(accessToken, refreshToken);
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
        return createReminderAt(
            authorizationHeader,
            petId,
            "体内驱虫提醒",
            OffsetDateTime.parse("2026-04-18T09:00:00+08:00")
        );
    }

    private String createReminderAt(
        String authorizationHeader,
        String petId,
        String title,
        OffsetDateTime dueAt
    ) throws Exception {
        MvcResult createReminderResult = mockMvc.perform(post("/api/v1/pets/%s/reminders".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reminder_type": "deworming",
                      "title": "%s",
                      "due_at": "%s",
                      "notes": "饭后执行"
                    }
                    """.formatted(title, dueAt)))
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
        return createDailyLog(authorizationHeader, petId, content, false);
    }

    private String createDailyLog(
        String authorizationHeader,
        String petId,
        String content,
        boolean syncToCommunity
    ) throws Exception {
        return createDailyLogWithTagsAt(
            authorizationHeader,
            petId,
            content,
            List.of("晒太阳", "成长"),
            syncToCommunity,
            OffsetDateTime.parse("2026-04-17T10:00:00+08:00")
        );
    }

    private String createDailyLogWithTagsAt(
        String authorizationHeader,
        String petId,
        String content,
        List<String> tags,
        boolean syncToCommunity,
        OffsetDateTime happenedAt
    ) throws Exception {
        MvcResult createDailyLogResult = mockMvc.perform(post("/api/v1/pets/%s/daily-logs".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "%s",
                      "tags": %s,
                      "visibility": "public",
                      "sync_to_community": %s,
                      "happened_at": "%s"
                    }
                    """.formatted(content, toJsonArray(tags), syncToCommunity, happenedAt)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createDailyLogResult.getResponse().getContentAsString(), "$.data.daily_log_id");
    }

    private String createWeightHealthRecordAt(
        String authorizationHeader,
        String petId,
        String title,
        OffsetDateTime occurredAt,
        String value,
        String notes
    ) throws Exception {
        return createHealthRecordAt(
            authorizationHeader,
            petId,
            "weight",
            title,
            occurredAt,
            value,
            "kg",
            notes
        );
    }

    private String createMedicationHealthRecordAt(
        String authorizationHeader,
        String petId,
        String title,
        OffsetDateTime occurredAt,
        String notes
    ) throws Exception {
        return createHealthRecordAt(
            authorizationHeader,
            petId,
            "medication",
            title,
            occurredAt,
            null,
            null,
            notes
        );
    }

    private String createHealthRecordAt(
        String authorizationHeader,
        String petId,
        String recordType,
        String title,
        OffsetDateTime occurredAt,
        String value,
        String unit,
        String notes
    ) throws Exception {
        MvcResult createHealthRecordResult = mockMvc.perform(post("/api/v1/pets/%s/health-records".formatted(petId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "record_type": "%s",
                      "title": "%s",
                      "value": %s,
                      "unit": %s,
                      "occurred_at": "%s",
                      "notes": "%s"
                    }
                    """.formatted(
                    recordType,
                    title,
                    value == null ? "null" : "\"%s\"".formatted(value),
                    unit == null ? "null" : "\"%s\"".formatted(unit),
                    occurredAt,
                    notes
                )))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(createHealthRecordResult.getResponse().getContentAsString(), "$.data.health_record_id");
    }

    private String toJsonArray(List<String> values) {
        return values.stream()
            .map(value -> "\"%s\"".formatted(value))
            .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    private String currentCommunityPostId(String authorizationHeader) throws Exception {
        MvcResult feedResult = mockMvc.perform(get("/api/v1/community/feed?tab=recommended")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(feedResult.getResponse().getContentAsString(), "$.data[0].post_id");
    }

    private String createCommunityPostReport(
        String authorizationHeader,
        String postId,
        String reasonCode,
        String reasonDetail
    ) throws Exception {
        MvcResult reportResult = mockMvc.perform(post("/api/v1/community/posts/%s/report".formatted(postId))
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason_code": "%s",
                      "reason_detail": "%s"
                    }
                    """.formatted(reasonCode, reasonDetail)))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(reportResult.getResponse().getContentAsString(), "$.data.report_id");
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

    private record LoginTokensFixture(
        String accessToken,
        String refreshToken
    ) {
        private String authorizationHeader() {
            return "Bearer " + accessToken;
        }
    }
}
