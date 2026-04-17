package com.petlife.server.modules.dailylog.service;

import com.petlife.server.bootstrap.devsupport.BootstrapMemoryStore;
import com.petlife.server.bootstrap.devsupport.model.DevDailyLog;
import com.petlife.server.modules.dailylog.dto.request.CreateDailyLogRequest;
import com.petlife.server.modules.dailylog.dto.response.DailyLogResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 萌宠日常应用服务。
 *
 * <p>萌宠日常既要服务用户私有记录，又要为后续社区公开发布提供内容来源，因此在服务层统一处理
 * 内容、标签和可见范围映射，避免前端被迫感知后续领域拆分。</p>
 */
@Service
public class DailyLogApplicationService {

    private final BootstrapMemoryStore bootstrapMemoryStore;

    public DailyLogApplicationService(BootstrapMemoryStore bootstrapMemoryStore) {
        this.bootstrapMemoryStore = bootstrapMemoryStore;
    }

    public List<DailyLogResponse> listDailyLogs(Long petId) {
        return bootstrapMemoryStore.listDailyLogs(petId).stream()
            .map(this::toDailyLogResponse)
            .toList();
    }

    public DailyLogResponse createDailyLog(Long petId, CreateDailyLogRequest request) {
        DevDailyLog dailyLog = bootstrapMemoryStore.createDailyLog(
            petId,
            request.content(),
            request.tags() == null ? List.of() : request.tags(),
            request.visibility(),
            request.happenedAt()
        );
        return toDailyLogResponse(dailyLog);
    }

    private DailyLogResponse toDailyLogResponse(DevDailyLog dailyLog) {
        return new DailyLogResponse(
            String.valueOf(dailyLog.dailyLogId()),
            String.valueOf(dailyLog.petId()),
            dailyLog.content(),
            dailyLog.tags(),
            dailyLog.visibility(),
            dailyLog.happenedAt(),
            dailyLog.createdAt()
        );
    }
}
