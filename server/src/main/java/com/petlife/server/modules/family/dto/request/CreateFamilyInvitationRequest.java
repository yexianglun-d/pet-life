package com.petlife.server.modules.family.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 家庭邀请创建请求。
 *
 * @param inviteeMobile 被邀请人手机号
 * @param role 邀请角色
 * @param sharedPetIds 共享宠物 ID 列表
 */
public record CreateFamilyInvitationRequest(
    @NotBlank(message = "被邀请手机号不能为空")
    String inviteeMobile,
    @NotBlank(message = "邀请角色不能为空")
    String role,
    @NotEmpty(message = "至少选择一只共享宠物")
    List<String> sharedPetIds
) {
}
