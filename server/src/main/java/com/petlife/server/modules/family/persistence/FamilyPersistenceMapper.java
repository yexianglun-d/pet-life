package com.petlife.server.modules.family.persistence;

import com.petlife.server.modules.family.persistence.command.CreateFamilyCommand;
import com.petlife.server.modules.family.persistence.command.CreateFamilyInvitationCommand;
import com.petlife.server.modules.family.persistence.dataobject.FamilyInvitationDataObject;
import com.petlife.server.modules.family.persistence.dataobject.FamilyMemberDataObject;
import com.petlife.server.modules.family.persistence.dataobject.FamilyProfileDataObject;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * 家庭共养持久化 Mapper。
 */
@Mapper
public interface FamilyPersistenceMapper {

    @Select("""
        SELECT
          f.id AS familyId,
          f.family_name AS familyName,
          f.owner_user_id AS ownerUserId,
          COUNT(all_members.id) AS memberCount,
          current_member.role AS currentUserRole
        FROM family_members current_member
        JOIN families f ON f.id = current_member.family_id
        LEFT JOIN user_settings us ON us.user_id = current_member.user_id
        LEFT JOIN pets current_pet
          ON current_pet.id = us.current_pet_id
         AND current_pet.deleted_at IS NULL
         AND current_pet.status = 'active'
        LEFT JOIN family_members all_members
          ON all_members.family_id = f.id
         AND all_members.invite_status = 'joined'
        WHERE current_member.user_id = #{userId}
          AND current_member.invite_status = 'joined'
          AND f.deleted_at IS NULL
          AND f.status = 1
        GROUP BY f.id, f.family_name, f.owner_user_id, current_member.role, current_member.joined_at
        ORDER BY CASE
          WHEN current_pet.family_id = f.id THEN 0
          ELSE 1
        END ASC,
        CASE current_member.role
          WHEN 'owner' THEN 1
          WHEN 'admin' THEN 2
          ELSE 3
        END ASC, current_member.joined_at DESC, f.id ASC
        LIMIT 1
        """)
    FamilyProfileDataObject findAccessibleFamilyProfileByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT
          f.id AS familyId,
          f.family_name AS familyName,
          f.owner_user_id AS ownerUserId,
          COUNT(all_members.id) AS memberCount,
          NULL AS currentUserRole
        FROM families f
        LEFT JOIN family_members all_members
          ON all_members.family_id = f.id
         AND all_members.invite_status = 'joined'
        WHERE f.id = #{familyId}
          AND f.deleted_at IS NULL
          AND f.status = 1
        GROUP BY f.id, f.family_name, f.owner_user_id
        LIMIT 1
        """)
    FamilyProfileDataObject findFamilyProfileById(@Param("familyId") Long familyId);

    @Insert("""
        INSERT INTO families (
          family_name, owner_user_id, status, created_at, updated_at
        ) VALUES (
          #{familyName}, #{ownerUserId}, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFamily(CreateFamilyCommand command);

    @Insert("""
        INSERT INTO family_members (
          family_id, user_id, role, invite_status, joined_at, created_at, updated_at
        ) VALUES (
          #{familyId}, #{userId}, #{role}, 'joined', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        ON DUPLICATE KEY UPDATE invite_status = 'joined',
                                role = VALUES(role),
                                joined_at = COALESCE(joined_at, CURRENT_TIMESTAMP),
                                updated_at = CURRENT_TIMESTAMP
        """)
    int insertFamilyMember(
        @Param("familyId") Long familyId,
        @Param("userId") Long userId,
        @Param("role") String role
    );

    @Select("""
        SELECT
          fm.id AS memberId,
          fm.family_id AS familyId,
          fm.user_id AS userId,
          u.nickname AS nickname,
          u.mobile AS mobile,
          fm.role AS role,
          fm.invite_status AS inviteStatus,
          fm.joined_at AS joinedAt
        FROM family_members fm
        JOIN users u ON u.id = fm.user_id
        WHERE fm.family_id = #{familyId}
          AND fm.invite_status = 'joined'
          AND u.deleted_at IS NULL
          AND u.status = 1
        ORDER BY CASE fm.role
          WHEN 'owner' THEN 1
          WHEN 'admin' THEN 2
          ELSE 3
        END ASC, fm.id ASC
        """)
    List<FamilyMemberDataObject> listJoinedMembersByFamilyId(@Param("familyId") Long familyId);

    @Select("""
        SELECT
          fm.id AS memberId,
          fm.family_id AS familyId,
          fm.user_id AS userId,
          u.nickname AS nickname,
          u.mobile AS mobile,
          fm.role AS role,
          fm.invite_status AS inviteStatus,
          fm.joined_at AS joinedAt
        FROM family_members fm
        JOIN users u ON u.id = fm.user_id
        WHERE fm.id = #{memberId}
          AND fm.invite_status = 'joined'
          AND u.deleted_at IS NULL
          AND u.status = 1
        LIMIT 1
        """)
    FamilyMemberDataObject findJoinedMemberById(@Param("memberId") Long memberId);

    @Select("""
        SELECT
          fm.id AS memberId,
          fm.family_id AS familyId,
          fm.user_id AS userId,
          u.nickname AS nickname,
          u.mobile AS mobile,
          fm.role AS role,
          fm.invite_status AS inviteStatus,
          fm.joined_at AS joinedAt
        FROM family_members fm
        JOIN users u ON u.id = fm.user_id
        WHERE fm.family_id = #{familyId}
          AND fm.user_id = #{userId}
          AND fm.invite_status = 'joined'
          AND u.deleted_at IS NULL
          AND u.status = 1
        LIMIT 1
        """)
    FamilyMemberDataObject findJoinedMemberByFamilyAndUserId(
        @Param("familyId") Long familyId,
        @Param("userId") Long userId
    );

    @Select("""
        SELECT
          id AS invitationId,
          family_id AS familyId,
          inviter_user_id AS inviterUserId,
          invitee_mobile AS inviteeMobile,
          invitee_user_id AS inviteeUserId,
          role AS role,
          shared_pet_ids AS sharedPetIdsJson,
          invite_code AS inviteCode,
          status AS status,
          expired_at AS expiredAt,
          accepted_at AS acceptedAt,
          created_at AS createdAt
        FROM family_invitations
        WHERE family_id = #{familyId}
          AND status = 'pending'
        ORDER BY created_at DESC, id DESC
        """)
    List<FamilyInvitationDataObject> listPendingInvitationsByFamilyId(@Param("familyId") Long familyId);

    @Insert("""
        INSERT INTO family_invitations (
          family_id, inviter_user_id, invitee_mobile, invitee_user_id, role, shared_pet_ids,
          invite_code, status, expired_at, created_at, updated_at
        ) VALUES (
          #{familyId}, #{inviterUserId}, #{inviteeMobile}, #{inviteeUserId}, #{role}, #{sharedPetIdsJson},
          #{inviteCode}, 'pending', #{expiredAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFamilyInvitation(CreateFamilyInvitationCommand command);

    @Select("""
        SELECT
          id AS invitationId,
          family_id AS familyId,
          inviter_user_id AS inviterUserId,
          invitee_mobile AS inviteeMobile,
          invitee_user_id AS inviteeUserId,
          role AS role,
          shared_pet_ids AS sharedPetIdsJson,
          invite_code AS inviteCode,
          status AS status,
          expired_at AS expiredAt,
          accepted_at AS acceptedAt,
          created_at AS createdAt
        FROM family_invitations
        WHERE id = #{invitationId}
        LIMIT 1
        """)
    FamilyInvitationDataObject findInvitationById(@Param("invitationId") Long invitationId);

    @Select("""
        SELECT
          id AS invitationId,
          family_id AS familyId,
          inviter_user_id AS inviterUserId,
          invitee_mobile AS inviteeMobile,
          invitee_user_id AS inviteeUserId,
          role AS role,
          shared_pet_ids AS sharedPetIdsJson,
          invite_code AS inviteCode,
          status AS status,
          expired_at AS expiredAt,
          accepted_at AS acceptedAt,
          created_at AS createdAt
        FROM family_invitations
        WHERE invite_code = #{inviteCode}
        LIMIT 1
        """)
    FamilyInvitationDataObject findInvitationByCode(@Param("inviteCode") String inviteCode);

    @Select("""
        SELECT
          id AS invitationId,
          family_id AS familyId,
          inviter_user_id AS inviterUserId,
          invitee_mobile AS inviteeMobile,
          invitee_user_id AS inviteeUserId,
          role AS role,
          shared_pet_ids AS sharedPetIdsJson,
          invite_code AS inviteCode,
          status AS status,
          expired_at AS expiredAt,
          accepted_at AS acceptedAt,
          created_at AS createdAt
        FROM family_invitations
        WHERE family_id = #{familyId}
          AND invitee_user_id = #{userId}
          AND status = 'accepted'
        ORDER BY accepted_at DESC, id DESC
        LIMIT 1
        """)
    FamilyInvitationDataObject findLatestAcceptedInvitationByFamilyAndUserId(
        @Param("familyId") Long familyId,
        @Param("userId") Long userId
    );

    @Update("""
        UPDATE family_invitations
        SET status = 'accepted',
            invitee_user_id = #{inviteeUserId},
            accepted_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{invitationId}
          AND status = 'pending'
          AND (expired_at IS NULL OR expired_at > CURRENT_TIMESTAMP)
        """)
    int acceptInvitation(
        @Param("invitationId") Long invitationId,
        @Param("inviteeUserId") Long inviteeUserId
    );

    @Update("""
        UPDATE family_invitations
        SET status = 'rejected',
            invitee_user_id = COALESCE(invitee_user_id, #{inviteeUserId}),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{invitationId}
          AND status = 'pending'
          AND (expired_at IS NULL OR expired_at > CURRENT_TIMESTAMP)
        """)
    int rejectInvitation(
        @Param("invitationId") Long invitationId,
        @Param("inviteeUserId") Long inviteeUserId
    );

    @Update("""
        UPDATE family_invitations
        SET status = 'expired',
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{invitationId}
          AND status = 'pending'
        """)
    int expireInvitation(@Param("invitationId") Long invitationId);

    @Update("""
        UPDATE family_members
        SET role = #{role},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{memberId}
          AND invite_status = 'joined'
        """)
    int updateFamilyMemberRole(
        @Param("memberId") Long memberId,
        @Param("role") String role
    );

    @Delete("""
        DELETE FROM family_members
        WHERE id = #{memberId}
          AND invite_status = 'joined'
        """)
    int deleteFamilyMember(@Param("memberId") Long memberId);
}
