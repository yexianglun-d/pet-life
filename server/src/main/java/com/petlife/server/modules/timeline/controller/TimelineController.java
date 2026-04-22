package com.petlife.server.modules.timeline.controller;

import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.modules.timeline.dto.response.TimelineEventResponse;
import com.petlife.server.modules.timeline.service.TimelineApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成长时间轴控制器。
 */
@RestController
@RequestMapping("/api/v1/pets/{petId}/timeline")
public class TimelineController {

    private final TimelineApplicationService timelineApplicationService;

    public TimelineController(TimelineApplicationService timelineApplicationService) {
        this.timelineApplicationService = timelineApplicationService;
    }

    @GetMapping
    public ApiResponse<List<TimelineEventResponse>> listTimelineEvents(
        @PathVariable Long petId,
        @RequestParam(name = "event_type", required = false) String eventType
    ) {
        return ApiResponse.success(timelineApplicationService.listTimelineEvents(petId, eventType));
    }
}
