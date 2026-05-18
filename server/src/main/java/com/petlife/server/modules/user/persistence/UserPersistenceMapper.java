package com.petlife.server.modules.user.persistence;

import com.petlife.server.modules.user.persistence.command.CreateFamilyCommand;
import com.petlife.server.modules.user.persistence.command.CreateUserCommand;
import com.petlife.server.modules.user.persistence.command.UpdateUserCityCommand;
import com.petlife.server.modules.user.persistence.command.UpdateUserNotificationSettingsCommand;
import com.petlife.server.modules.user.persistence.command.UpdateUserProfileCommand;
import com.petlife.server.modules.user.persistence.dataobject.AdminUserDataObject;
import com.petlife.server.modules.user.persistence.dataobject.FamilySummaryDataObject;
import com.petlife.server.modules.user.persistence.dataobject.UserProfileDataObject;
import com.petlife.server.modules.user.persistence.dataobject.UserSettingsDataObject;
import java.util.List;
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
          u.id AS userId,
          u.mobile AS mobile,
          u.nickname AS nickname,
          u.city_code AS cityCode,
          u.city_name AS cityName,
          us.current_pet_id AS currentPetId,
          us.notification_switch AS notificationSwitch,
          us.privacy_level AS privacyLevel
        FROM users u
        LEFT JOIN user_settings us ON us.user_id = u.id
        WHERE u.id = #{userId}
          AND u.deleted_at IS NULL
          AND u.status = 1
        LIMIT 1
        """)
    UserSettingsDataObject findUserSettingsByUserId(@Param("userId") Long userId);

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

    @Select("""
        SELECT
          u.id AS userId,
          u.mobile AS mobile,
          u.nickname AS nickname,
          u.avatar_url AS avatarUrl,
          u.city_code AS cityCode,
          u.city_name AS cityName,
          u.status AS status,
          u.last_login_at AS lastLoginAt,
          u.created_at AS createdAt,
          us.current_pet_id AS currentPetId,
          COALESCE(us.notification_switch, 1) AS notificationSwitch,
          COALESCE(us.privacy_level, 'normal') AS privacyLevel,
          primary_family.id AS familyId,
          primary_family.family_name AS familyName,
          primary_member.role AS familyRole,
          (
            SELECT COUNT(1)
            FROM family_members joined_member
            WHERE joined_member.family_id = primary_family.id
              AND joined_member.invite_status = 'joined'
          ) AS familyMemberCount,
          current_pet.family_id AS currentPetFamilyId,
          current_pet_family.family_name AS currentPetFamilyName,
          current_pet.pet_name AS currentPetName,
          current_pet.pet_type AS currentPetType,
          current_pet.owner_user_id AS currentPetOwnerUserId,
          current_pet_owner.nickname AS currentPetOwnerNickname,
          current_pet_owner.mobile AS currentPetOwnerMobile,
          (
            SELECT COUNT(DISTINCT accessible_pet.id)
            FROM pets accessible_pet
            WHERE accessible_pet.deleted_at IS NULL
              AND accessible_pet.status = 'active'
              AND (
                accessible_pet.owner_user_id = u.id
                OR EXISTS (
                  SELECT 1
                  FROM family_members accessible_member
                  WHERE accessible_member.family_id = accessible_pet.family_id
                    AND accessible_member.user_id = u.id
                    AND accessible_member.invite_status = 'joined'
                )
              )
          ) AS petCount
        FROM users u
        LEFT JOIN user_settings us ON us.user_id = u.id
        LEFT JOIN pets current_pet
          ON current_pet.id = us.current_pet_id
         AND current_pet.deleted_at IS NULL
         AND current_pet.status = 'active'
        LEFT JOIN families current_pet_family
          ON current_pet_family.id = current_pet.family_id
         AND current_pet_family.deleted_at IS NULL
        LEFT JOIN users current_pet_owner ON current_pet_owner.id = current_pet.owner_user_id
        LEFT JOIN family_members primary_member
          ON primary_member.id = (
            SELECT fm.id
            FROM family_members fm
            JOIN families family_scope
              ON family_scope.id = fm.family_id
             AND family_scope.deleted_at IS NULL
             AND family_scope.status = 1
            WHERE fm.user_id = u.id
              AND fm.invite_status = 'joined'
            ORDER BY CASE WHEN current_pet.family_id = fm.family_id THEN 0 ELSE 1 END ASC,
                     CASE fm.role WHEN 'owner' THEN 1 WHEN 'admin' THEN 2 ELSE 3 END ASC,
                     fm.joined_at DESC,
                     fm.id DESC
            LIMIT 1
          )
        LEFT JOIN families primary_family
          ON primary_family.id = primary_member.family_id
         AND primary_family.deleted_at IS NULL
        WHERE u.deleted_at IS NULL
          AND (
            #{keyword} IS NULL
            OR u.mobile LIKE CONCAT('%', #{keyword}, '%')
            OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
            OR u.city_name LIKE CONCAT('%', #{keyword}, '%')
            OR current_pet.pet_name LIKE CONCAT('%', #{keyword}, '%')
            OR primary_family.family_name LIKE CONCAT('%', #{keyword}, '%')
          )
          AND (#{mobile} IS NULL OR u.mobile LIKE CONCAT('%', #{mobile}, '%'))
          AND (#{nickname} IS NULL OR u.nickname LIKE CONCAT('%', #{nickname}, '%'))
          AND (#{cityCode} IS NULL OR u.city_code = #{cityCode})
          AND (#{notificationSwitch} IS NULL OR COALESCE(us.notification_switch, 1) = #{notificationSwitch})
          AND (#{privacyLevel} IS NULL OR COALESCE(us.privacy_level, 'normal') = #{privacyLevel})
        ORDER BY u.created_at DESC, u.id DESC
        LIMIT 200
        """)
    List<AdminUserDataObject> listAdminUsers(
        @Param("keyword") String keyword,
        @Param("mobile") String mobile,
        @Param("nickname") String nickname,
        @Param("cityCode") String cityCode,
        @Param("notificationSwitch") Integer notificationSwitch,
        @Param("privacyLevel") String privacyLevel
    );

    @Select("""
        SELECT
          u.id AS userId,
          u.mobile AS mobile,
          u.nickname AS nickname,
          u.avatar_url AS avatarUrl,
          u.city_code AS cityCode,
          u.city_name AS cityName,
          u.status AS status,
          u.last_login_at AS lastLoginAt,
          u.created_at AS createdAt,
          us.current_pet_id AS currentPetId,
          COALESCE(us.notification_switch, 1) AS notificationSwitch,
          COALESCE(us.privacy_level, 'normal') AS privacyLevel,
          primary_family.id AS familyId,
          primary_family.family_name AS familyName,
          primary_member.role AS familyRole,
          (
            SELECT COUNT(1)
            FROM family_members joined_member
            WHERE joined_member.family_id = primary_family.id
              AND joined_member.invite_status = 'joined'
          ) AS familyMemberCount,
          current_pet.family_id AS currentPetFamilyId,
          current_pet_family.family_name AS currentPetFamilyName,
          current_pet.pet_name AS currentPetName,
          current_pet.pet_type AS currentPetType,
          current_pet.owner_user_id AS currentPetOwnerUserId,
          current_pet_owner.nickname AS currentPetOwnerNickname,
          current_pet_owner.mobile AS currentPetOwnerMobile,
          (
            SELECT COUNT(DISTINCT accessible_pet.id)
            FROM pets accessible_pet
            WHERE accessible_pet.deleted_at IS NULL
              AND accessible_pet.status = 'active'
              AND (
                accessible_pet.owner_user_id = u.id
                OR EXISTS (
                  SELECT 1
                  FROM family_members accessible_member
                  WHERE accessible_member.family_id = accessible_pet.family_id
                    AND accessible_member.user_id = u.id
                    AND accessible_member.invite_status = 'joined'
                )
              )
          ) AS petCount
        FROM users u
        LEFT JOIN user_settings us ON us.user_id = u.id
        LEFT JOIN pets current_pet
          ON current_pet.id = us.current_pet_id
         AND current_pet.deleted_at IS NULL
         AND current_pet.status = 'active'
        LEFT JOIN families current_pet_family
          ON current_pet_family.id = current_pet.family_id
         AND current_pet_family.deleted_at IS NULL
        LEFT JOIN users current_pet_owner ON current_pet_owner.id = current_pet.owner_user_id
        LEFT JOIN family_members primary_member
          ON primary_member.id = (
            SELECT fm.id
            FROM family_members fm
            JOIN families family_scope
              ON family_scope.id = fm.family_id
             AND family_scope.deleted_at IS NULL
             AND family_scope.status = 1
            WHERE fm.user_id = u.id
              AND fm.invite_status = 'joined'
            ORDER BY CASE WHEN current_pet.family_id = fm.family_id THEN 0 ELSE 1 END ASC,
                     CASE fm.role WHEN 'owner' THEN 1 WHEN 'admin' THEN 2 ELSE 3 END ASC,
                     fm.joined_at DESC,
                     fm.id DESC
            LIMIT 1
          )
        LEFT JOIN families primary_family
          ON primary_family.id = primary_member.family_id
         AND primary_family.deleted_at IS NULL
        WHERE u.id = #{userId}
          AND u.deleted_at IS NULL
        LIMIT 1
        """)
    AdminUserDataObject findAdminUserById(@Param("userId") Long userId);

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
        SET nickname = #{nickname},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{userId}
        """)
    int updateUserProfile(UpdateUserProfileCommand command);

    @Update("""
        UPDATE users
        SET city_code = #{cityCode},
            city_name = #{cityName},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{userId}
        """)
    int updateUserCity(UpdateUserCityCommand command);

    @Update("""
        UPDATE user_settings
        SET notification_switch = #{notificationSwitch},
            privacy_level = #{privacyLevel},
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = #{userId}
        """)
    int updateUserNotificationSettings(UpdateUserNotificationSettingsCommand command);

    @Update("""
        UPDATE users
        SET last_login_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{userId}
        """)
    int updateLastLoginAt(@Param("userId") Long userId);
}
