package com.petlife.server.modules.reminder.service;

import com.petlife.server.bootstrap.devsupport.BootstrapMemoryStore;
import com.petlife.server.bootstrap.devsupport.model.DevReminder;
import com.petlife.server.modules.reminder.dto.request.CreateReminderRequest;
import com.petlife.server.modules.reminder.dto.response.ReminderResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 提醒应用服务。
 *
 * <p>提醒模块承担首页待办、宠物主页待办和后续通知中心的数据出口，因此在应用层统一封装
 * 创建、查询和完成动作，保证后续扩展消息发送逻辑时不侵入控制器。</p>
 */
@Service
public class ReminderApplicationService {

    private final BootstrapMemoryStore bootstrapMemoryStore;

    public ReminderApplicationService(BootstrapMemoryStore bootstrapMemoryStore) {
        this.bootstrapMemoryStore = bootstrapMemoryStore;
    }

    public List<ReminderResponse> listReminders(Long petId) {
        return bootstrapMemoryStore.listReminders(petId).stream()
            .map(this::toReminderResponse)
            .toList();
    }

    public ReminderResponse createReminder(Long petId, CreateReminderRequest request) {
        DevReminder reminder = bootstrapMemoryStore.createReminder(
            petId,
            request.reminderType(),
            request.title(),
            request.dueAt(),
            request.notes()
        );
        return toReminderResponse(reminder);
    }

    public ReminderResponse completeReminder(Long petId, Long reminderId) {
        return toReminderResponse(bootstrapMemoryStore.completeReminder(petId, reminderId));
    }

    private ReminderResponse toReminderResponse(DevReminder reminder) {
        return new ReminderResponse(
            String.valueOf(reminder.reminderId()),
            String.valueOf(reminder.petId()),
            reminder.reminderType(),
            reminder.title(),
            reminder.dueAt(),
            reminder.status(),
            reminder.notes(),
            reminder.completedAt(),
            reminder.createdAt()
        );
    }
}
