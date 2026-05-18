package com.petlife.server.modules.timeline.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.timeline.dto.response.AdminTimelineEventResponse;
import com.petlife.server.modules.timeline.service.TimelineApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台时间轴查询控制器。
 */
@RestController
@RequestMapping("/api/v1/admin/timeline")
public class AdminTimelineController {

    private final TimelineApplicationService timelineApplicationService;

    public AdminTimelineController(TimelineApplicationService timelineApplicationService) {
        this.timelineApplicationService = timelineApplicationService;
    }

    @GetMapping("/events")
    public ApiResponse<List<AdminTimelineEventResponse>> listTimelineEvents(
        @RequestParam(value = "event_type", required = false) String eventType,
        @RequestParam(value = "source_type", required = false) String sourceType,
        @RequestParam(value = "pet_id", required = false) Long petId,
        @RequestParam(value = "source_id", required = false) Long sourceId
    ) {
        return ApiResponse.success(
            timelineApplicationService.listAdminTimelineEvents(eventType, sourceType, petId, sourceId)
        );
    }

    @GetMapping("/events/{eventId}")
    public ApiResponse<AdminTimelineEventResponse> getTimelineEvent(@PathVariable Long eventId) {
        return ApiResponse.success(timelineApplicationService.getAdminTimelineEvent(eventId));
    }
}
