package com.petlife.server.modules.user.persistence;

import com.petlife.server.modules.user.persistence.command.CreateFamilyCommand;
import com.petlife.server.modules.user.persistence.command.CreateUserCommand;
import com.petlife.server.modules.user.persistence.dataobject.FamilySummaryDataObject;
import com.petlife.server.modules.user.persistence.dataobject.UserProfileDataObject;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户资料与设置持久化 Mapper。
 */
@Mapper
public interface UserPersistenceMapper {

    @Insert("""
        INSERT INTO users (
          mobile, nickname, city_code, city_name, status, last_login_at, created_at, updated_at
        ) VALUES (
          #{mobile}, #{nickname}, #{cityCode}, #{cityName}, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(CreateUserCommand command);

    @Insert("""
        INSERT INTO user_settings (
          user_id, current_pet_id, notification_switch, privacy_level, created_at, updated_at
        ) VALUES (
          #{userId}, NULL, 1, 'normal', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        ON DUPLICATE KEY UPDATE updated_at = updated_at
        """)
    int insertUserSettingsIfAbsent(@Param("userId") Long userId);

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
                                updated_at = CURRENT_TIMESTAMP
        """)
    int insertFamilyMember(
        @Param("familyId") Long familyId,
        @Param("userId") Long userId,
        @Param("role") String role
    );

    @Select("""
        SELECT
          u.id AS userId,
          u.mobile AS mobile,
          u.nickname AS nickname,
          u.avatar_url AS avatarUrl,
          u.city_code AS cityCode,
          u.city_name AS cityName,
          us.current_pet_id AS currentPetId
        FROM users u
        LEFT JOIN user_settings us ON us.user_id = u.id
        WHERE u.id = #{userId}
          AND u.deleted_at IS NULL
          AND u.status = 1
        LIMIT 1
        """)
    UserProfileDataObject findUserProfileById(@Param("userId") Long userId);

    @Select("""
        SELECT
          u.id AS userId,
          u.mobile AS mobile,
          u.nickname AS nickname,
          u.avatar_url AS avatarUrl,
          u.city_code AS cityCode,
          u.city_name AS cityName,
          us.current_pet_id AS currentPetId
        FROM users u
        LEFT JOIN user_settings us ON us.user_id = u.id
        WHERE u.mobile = #{mobile}
          AND u.deleted_at IS NULL
          AND u.status = 1
        LIMIT 1
        """)
    UserProfileDataObject findUserProfileByMobile(@Param("mobile") String mobile);

    @Select("""
        SELECT
          f.id AS familyId,
          f.family_name AS familyName,
          COUNT(all_members.id) AS memberCount,
          current_member.role AS role
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
        GROUP BY f.id, f.family_name, current_member.role, current_member.joined_at
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
    FamilySummaryDataObject findPrimaryFamilySummaryByUserId(@Param("userId") Long userId);

    @Update("""
        UPDATE user_settings
        SET current_pet_id = #{petId},
            updated_at = CURRENT_TIMESTAMP(3)
        WHERE user_id = #{userId}
        """)
    int updateCurrentPet(
        @Param("userId") Long userId,
        @Param("petId") Long petId
    );

    @Update("""
        UPDATE users
        SET last_login_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{userId}
        """)
    int updateLastLoginAt(@Param("userId") Long userId);
}
