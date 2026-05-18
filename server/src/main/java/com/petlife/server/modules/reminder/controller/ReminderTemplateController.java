package com.petlife.server.modules.reminder.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.reminder.dto.response.ReminderTemplateResponse;
import com.petlife.server.modules.reminder.service.ReminderTemplateApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端提醒模板控制器。
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/reminder-templates")
public class ReminderTemplateController {

    private final ReminderTemplateApplicationService reminderTemplateApplicationService;

    public ReminderTemplateController(ReminderTemplateApplicationService reminderTemplateApplicationService) {
        this.reminderTemplateApplicationService = reminderTemplateApplicationService;
    }

    @GetMapping
    public ApiResponse<List<ReminderTemplateResponse>> listTemplates(@PathVariable Long petId) {
        return ApiResponse.success(reminderTemplateApplicationService.listUserTemplates(petId));
    }
}
