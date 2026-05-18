package com.petlife.server.modules.home.dto.response;

import com.petlife.server.modules.community.dto.response.CommunityPostResponse;
import com.petlife.server.modules.user.dto.response.CurrentUserResponse;
import java.util.List;

/**
 * 首页聚合响应。
 *
 * @param currentUser 当前用户上下文
 * @param dashboard 当前宠物首页面板；无当前宠物时为空
 * @param quickActions 快捷记录配置
 * @param communityRecommendations 社区推荐内容
 * @param serviceEntries 服务入口
 * @param deviceSummary 设备摘要
 */
public record HomeAggregateResponse(
    CurrentUserResponse currentUser,
    HomePetDashboardResponse dashboard,
    List<HomeQuickActionResponse> quickActions,
    List<CommunityPostResponse> communityRecommendations,
    List<HomeServiceEntryResponse> serviceEntries,
    HomeDeviceSummaryResponse deviceSummary
) {
}
