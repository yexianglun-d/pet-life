package com.petlife.server.modules.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 独立社区帖子发布请求。
 *
 * @param petId 关联宠物 ID，可为空
 * @param topicId 关联话题 ID，可为空
 * @param postType 帖子类型：image_text/video/qa/experience
 * @param title 帖子标题
 * @param content 帖子正文
 * @param mediaAssetIds 媒体资产 ID
 * @param cityCode 城市编码
 * @param visibility 可见性：public/follower
 */
public record CreateCommunityPostRequest(
    Long petId,
    Long topicId,
    @Size(max = 30, message = "帖子类型长度不能超过 30 个字符")
    String postType,
    @Size(max = 100, message = "帖子标题不能超过 100 字")
    String title,
    @NotBlank(message = "帖子正文不能为空")
    String content,
    @Size(max = 9, message = "社区帖子最多上传 9 个媒体")
    List<String> mediaAssetIds,
    @Size(max = 32, message = "城市编码长度不能超过 32 个字符")
    String cityCode,
    @Size(max = 20, message = "可见性长度不能超过 20 个字符")
    String visibility
) {
}
