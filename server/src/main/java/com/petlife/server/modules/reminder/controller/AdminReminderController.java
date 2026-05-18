package com.petlife.server.modules.reminder.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.reminder.dto.response.AdminReminderResponse;
import com.petlife.server.modules.reminder.service.ReminderApplicationService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台提醒查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/reminders")
public class AdminReminderController {

    private final ReminderApplicationService reminderApplicationService;

    public AdminReminderController(ReminderApplicationService reminderApplicationService) {
        this.reminderApplicationService = reminderApplicationService;
    }

    @GetMapping
    public ApiResponse<List<AdminReminderResponse>> listReminders(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "reminder_type", required = false) String reminderType,
        @RequestParam(value = "reminder_mode", required = false) String reminderMode,
        @RequestParam(value = "pet_id", required = false) Long petId,
        @RequestParam(value = "family_id", required = false) Long familyId,
        @RequestParam(value = "owner_user_id", required = false) Long ownerUserId,
        @RequestParam(value = "handler_user_id", required = false) Long handlerUserId,
        @RequestParam(value = "source_record_id", required = false) Long sourceRecordId,
        @RequestParam(value = "due_from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dueFrom,
        @RequestParam(value = "due_to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dueTo
    ) {
        return ApiResponse.success(
            reminderApplicationService.listAdminReminders(
                keyword,
                status,
                reminderType,
                reminderMode,
                petId,
                familyId,
                ownerUserId,
                handlerUserId,
                sourceRecordId,
                dueFrom,
                dueTo
            )
        );
    }

    @GetMapping("/{reminderId}")
    public ApiResponse<AdminReminderResponse> getReminder(@PathVariable Long reminderId) {
        return ApiResponse.success(reminderApplicationService.getAdminReminder(reminderId));
    }
}
