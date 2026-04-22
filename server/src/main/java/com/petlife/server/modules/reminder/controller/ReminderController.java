package com.petlife.server.modules.reminder.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.reminder.dto.request.CreateReminderRequest;
import com.petlife.server.modules.reminder.dto.response.ReminderResponse;
import com.petlife.server.modules.reminder.service.ReminderApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提醒控制器。
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/reminders")
public class ReminderController {

    private final ReminderApplicationService reminderApplicationService;

    public ReminderController(ReminderApplicationService reminderApplicationService) {
        this.reminderApplicationService = reminderApplicationService;
    }

    @GetMapping
    public ApiResponse<List<ReminderResponse>> listReminders(@PathVariable Long petId) {
        return ApiResponse.success(reminderApplicationService.listReminders(petId));
    }

    @PostMapping
    public ApiResponse<ReminderResponse> createReminder(
        @PathVariable Long petId,
        @Valid @RequestBody CreateReminderRequest request
    ) {
        return ApiResponse.success(reminderApplicationService.createReminder(petId, request));
    }

    @PatchMapping("/{reminderId}/complete")
    public ApiResponse<ReminderResponse> completeReminder(
        @PathVariable Long petId,
        @PathVariable Long reminderId
    ) {
        return ApiResponse.success(reminderApplicationService.completeReminder(petId, reminderId));
    }

    @PatchMapping("/{reminderId}/skip")
    public ApiResponse<ReminderResponse> skipReminder(
        @PathVariable Long petId,
        @PathVariable Long reminderId
    ) {
        return ApiResponse.success(reminderApplicationService.skipReminder(petId, reminderId));
    }
}
