package com.petlife.server.modules.pet.persistence;

import com.petlife.server.modules.pet.persistence.command.ArchivePetCommand;
import com.petlife.server.modules.pet.persistence.command.CreatePetCommand;
import com.petlife.server.modules.pet.persistence.command.DeletePetCommand;
import com.petlife.server.modules.pet.persistence.command.UpdatePetProfileCommand;
import com.petlife.server.modules.pet.persistence.dataobject.AdminPetDataObject;
import com.petlife.server.modules.pet.persistence.dataobject.PetProfileDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 宠物主档持久化 Mapper。
 */
@Mapper
public interface PetPersistenceMapper {

    @Select("""
        SELECT
          p.id AS petId,
          p.family_id AS familyId,
          p.owner_user_id AS ownerUserId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS breed,
          p.gender AS gender,
          p.birthday AS birthday,
          p.adopt_date AS adoptDate,
          p.neuter_status AS neuterStatus,
          p.avatar_url AS avatarUrl,
          p.weight_kg AS weightKg,
          p.allergy_notes AS allergyNotes,
          p.medical_history AS medicalHistory,
          p.status AS status,
          p.created_at AS createdAt,
          p.updated_at AS updatedAt
        FROM pets p
        WHERE p.deleted_at IS NULL
          AND p.status = 'active'
          AND (
            p.owner_user_id = #{userId}
            OR EXISTS (
              SELECT 1
              FROM family_members fm
              LEFT JOIN family_invitations latest_invitation
                ON latest_invitation.id = (
                  SELECT fi.id
                  FROM family_invitations fi
                  WHERE fi.family_id = fm.family_id
                    AND fi.invitee_user_id = fm.user_id
                    AND fi.status = 'accepted'
                  ORDER BY fi.accepted_at DESC, fi.id DESC
                  LIMIT 1
                )
              WHERE fm.family_id = p.family_id
                AND fm.user_id = #{userId}
                AND fm.invite_status = 'joined'
                AND (
                  fm.role = 'owner'
                  OR latest_invitation.id IS NULL
                  OR EXISTS (
                    SELECT 1
                    FROM JSON_TABLE(
                      COALESCE(latest_invitation.shared_pet_ids, JSON_ARRAY()),
                      '$[*]' COLUMNS (shared_pet_id BIGINT PATH '$')
                    ) shared_pet_scope
                    WHERE shared_pet_scope.shared_pet_id = p.id
                  )
                )
            )
          )
        ORDER BY p.id ASC
        """)
    List<PetProfileDataObject> listPetsByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT
          p.id AS petId,
          p.family_id AS familyId,
          p.owner_user_id AS ownerUserId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS breed,
          p.gender AS gender,
          p.birthday AS birthday,
          p.adopt_date AS adoptDate,
          p.neuter_status AS neuterStatus,
          p.avatar_url AS avatarUrl,
          p.weight_kg AS weightKg,
          p.allergy_notes AS allergyNotes,
          p.medical_history AS medicalHistory,
          p.status AS status,
          p.created_at AS createdAt,
          p.updated_at AS updatedAt
        FROM pets p
        WHERE p.family_id = #{familyId}
          AND p.deleted_at IS NULL
          AND p.status = 'active'
        ORDER BY p.id ASC
        """)
    List<PetProfileDataObject> listPetsByFamilyId(@Param("familyId") Long familyId);

    @Select("""
        SELECT
          p.id AS petId,
          p.family_id AS familyId,
          p.owner_user_id AS ownerUserId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS breed,
          p.gender AS gender,
          p.birthday AS birthday,
          p.adopt_date AS adoptDate,
          p.neuter_status AS neuterStatus,
          p.avatar_url AS avatarUrl,
          p.weight_kg AS weightKg,
          p.allergy_notes AS allergyNotes,
          p.medical_history AS medicalHistory,
          p.status AS status,
          p.created_at AS createdAt,
          p.updated_at AS updatedAt
        FROM pets p
        WHERE p.id = #{petId}
          AND p.deleted_at IS NULL
          AND p.status = 'active'
        LIMIT 1
        """)
    PetProfileDataObject findPetById(@Param("petId") Long petId);

    @Select("""
        SELECT
          p.id AS petId,
          p.family_id AS familyId,
          p.owner_user_id AS ownerUserId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS breed,
          p.gender AS gender,
          p.birthday AS birthday,
          p.adopt_date AS adoptDate,
          p.neuter_status AS neuterStatus,
          p.avatar_url AS avatarUrl,
          p.weight_kg AS weightKg,
          p.allergy_notes AS allergyNotes,
          p.medical_history AS medicalHistory,
          p.status AS status,
          p.created_at AS createdAt,
          p.updated_at AS updatedAt
        FROM pets p
        WHERE p.id = #{petId}
          AND p.deleted_at IS NULL
          AND p.status = 'active'
          AND (
            p.owner_user_id = #{userId}
            OR EXISTS (
              SELECT 1
              FROM family_members fm
              LEFT JOIN family_invitations latest_invitation
                ON latest_invitation.id = (
                  SELECT fi.id
                  FROM family_invitations fi
                  WHERE fi.family_id = fm.family_id
                    AND fi.invitee_user_id = fm.user_id
                    AND fi.status = 'accepted'
                  ORDER BY fi.accepted_at DESC, fi.id DESC
                  LIMIT 1
                )
              WHERE fm.family_id = p.family_id
                AND fm.user_id = #{userId}
                AND fm.invite_status = 'joined'
                AND (
                  fm.role = 'owner'
                  OR latest_invitation.id IS NULL
                  OR EXISTS (
                    SELECT 1
                    FROM JSON_TABLE(
                      COALESCE(latest_invitation.shared_pet_ids, JSON_ARRAY()),
                      '$[*]' COLUMNS (shared_pet_id BIGINT PATH '$')
                    ) shared_pet_scope
                    WHERE shared_pet_scope.shared_pet_id = p.id
                  )
                )
            )
          )
        LIMIT 1
        """)
    PetProfileDataObject findAccessiblePetById(
        @Param("userId") Long userId,
        @Param("petId") Long petId
    );

    @Select("""
        SELECT
          p.id AS petId,
          p.family_id AS familyId,
          p.owner_user_id AS ownerUserId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS breed,
          p.gender AS gender,
          p.birthday AS birthday,
          p.adopt_date AS adoptDate,
          p.neuter_status AS neuterStatus,
          p.avatar_url AS avatarUrl,
          p.weight_kg AS weightKg,
          p.allergy_notes AS allergyNotes,
          p.medical_history AS medicalHistory,
          p.status AS status,
          p.created_at AS createdAt,
          p.updated_at AS updatedAt,
          f.family_name AS familyName,
          f.status AS familyStatus,
          (
            SELECT COUNT(1)
            FROM family_members joined_member
            WHERE joined_member.family_id = p.family_id
              AND joined_member.invite_status = 'joined'
          ) AS familyMemberCount,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile
        FROM pets p
        LEFT JOIN families f
          ON f.id = p.family_id
         AND f.deleted_at IS NULL
        LEFT JOIN users owner
          ON owner.id = p.owner_user_id
         AND owner.deleted_at IS NULL
        WHERE p.deleted_at IS NULL
          AND (
            #{keyword} IS NULL
            OR p.pet_name LIKE CONCAT('%', #{keyword}, '%')
            OR p.breed LIKE CONCAT('%', #{keyword}, '%')
            OR f.family_name LIKE CONCAT('%', #{keyword}, '%')
            OR owner.nickname LIKE CONCAT('%', #{keyword}, '%')
            OR owner.mobile LIKE CONCAT('%', #{keyword}, '%')
          )
          AND (#{petName} IS NULL OR p.pet_name LIKE CONCAT('%', #{petName}, '%'))
          AND (#{petType} IS NULL OR p.pet_type = #{petType})
          AND (#{status} IS NULL OR p.status = #{status})
          AND (#{ownerMobile} IS NULL OR owner.mobile LIKE CONCAT('%', #{ownerMobile}, '%'))
          AND (#{familyId} IS NULL OR p.family_id = #{familyId})
        ORDER BY p.updated_at DESC, p.id DESC
        LIMIT 200
        """)
    List<AdminPetDataObject> listAdminPets(
        @Param("keyword") String keyword,
        @Param("petName") String petName,
        @Param("petType") String petType,
        @Param("status") String status,
        @Param("ownerMobile") String ownerMobile,
        @Param("familyId") Long familyId
    );

    @Select("""
        SELECT
          p.id AS petId,
          p.family_id AS familyId,
          p.owner_user_id AS ownerUserId,
          p.pet_name AS petName,
          p.pet_type AS petType,
          p.breed AS breed,
          p.gender AS gender,
          p.birthday AS birthday,
          p.adopt_date AS adoptDate,
          p.neuter_status AS neuterStatus,
          p.avatar_url AS avatarUrl,
          p.weight_kg AS weightKg,
          p.allergy_notes AS allergyNotes,
          p.medical_history AS medicalHistory,
          p.status AS status,
          p.created_at AS createdAt,
          p.updated_at AS updatedAt,
          f.family_name AS familyName,
          f.status AS familyStatus,
          (
            SELECT COUNT(1)
            FROM family_members joined_member
            WHERE joined_member.family_id = p.family_id
              AND joined_member.invite_status = 'joined'
          ) AS familyMemberCount,
          owner.nickname AS ownerNickname,
          owner.mobile AS ownerMobile
        FROM pets p
        LEFT JOIN families f
          ON f.id = p.family_id
         AND f.deleted_at IS NULL
        LEFT JOIN users owner
          ON owner.id = p.owner_user_id
         AND owner.deleted_at IS NULL
        WHERE p.id = #{petId}
          AND p.deleted_at IS NULL
        LIMIT 1
        """)
    AdminPetDataObject findAdminPetById(@Param("petId") Long petId);

    @Insert("""
        INSERT INTO pets (
          family_id, owner_user_id, pet_name, pet_type, breed, gender, birthday, adopt_date,
          neuter_status, avatar_url, weight_kg, allergy_notes, medical_history, status, created_at, updated_at
        ) VALUES (
          #{familyId}, #{ownerUserId}, #{petName}, #{petType}, #{breed}, #{gender}, #{birthday}, #{adoptDate},
          #{neuterStatus}, #{avatarUrl}, #{weightKg}, #{allergyNotes}, #{medicalHistory},
          'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPet(CreatePetCommand command);

    @Update("""
        UPDATE pets
        SET pet_name = COALESCE(#{petName}, pet_name),
            pet_type = COALESCE(#{petType}, pet_type),
            breed = COALESCE(#{breed}, breed),
            gender = COALESCE(#{gender}, gender),
            birthday = COALESCE(#{birthday}, birthday),
            adopt_date = COALESCE(#{adoptDate}, adopt_date),
            neuter_status = COALESCE(#{neuterStatus}, neuter_status),
            avatar_url = COALESCE(#{avatarUrl}, avatar_url),
            weight_kg = COALESCE(#{weightKg}, weight_kg),
            allergy_notes = COALESCE(#{allergyNotes}, allergy_notes),
            medical_history = COALESCE(#{medicalHistory}, medical_history),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{petId}
          AND deleted_at IS NULL
          AND status = 'active'
          AND (
            owner_user_id = #{userId}
            OR EXISTS (
              SELECT 1
              FROM family_members fm
              LEFT JOIN family_invitations latest_invitation
                ON latest_invitation.id = (
                  SELECT fi.id
                  FROM family_invitations fi
                  WHERE fi.family_id = fm.family_id
                    AND fi.invitee_user_id = fm.user_id
                    AND fi.status = 'accepted'
                  ORDER BY fi.accepted_at DESC, fi.id DESC
                  LIMIT 1
                )
              WHERE fm.family_id = pets.family_id
                AND fm.user_id = #{userId}
                AND fm.invite_status = 'joined'
                AND (
                  fm.role = 'owner'
                  OR latest_invitation.id IS NULL
                  OR EXISTS (
                    SELECT 1
                    FROM JSON_TABLE(
                      COALESCE(latest_invitation.shared_pet_ids, JSON_ARRAY()),
                      '$[*]' COLUMNS (shared_pet_id BIGINT PATH '$')
                    ) shared_pet_scope
                    WHERE shared_pet_scope.shared_pet_id = pets.id
                  )
                )
            )
          )
        """)
    int updatePetSnapshot(UpdatePetProfileCommand command);

    @Update("""
        UPDATE pets
        SET status = #{archiveStatus},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{petId}
          AND deleted_at IS NULL
          AND status = 'active'
        """)
    int archivePet(ArchivePetCommand command);

    @Update("""
        UPDATE pets
        SET deleted_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{petId}
          AND deleted_at IS NULL
        """)
    int softDeletePet(DeletePetCommand command);

    @Select("""
        SELECT us.user_id
        FROM user_settings us
        WHERE us.current_pet_id = #{petId}
        ORDER BY us.user_id ASC
        """)
    List<Long> listUserIdsByCurrentPetId(@Param("petId") Long petId);

    @Select("""
        SELECT EXISTS(
          SELECT 1
          FROM pets owned_pet
          WHERE owned_pet.owner_user_id = #{userId}
          UNION
          SELECT 1
          FROM family_members fm
          JOIN pets family_pet ON family_pet.family_id = fm.family_id
          WHERE fm.user_id = #{userId}
            AND fm.invite_status = 'joined'
        )
        """)
    boolean existsPetHistoryByUserId(@Param("userId") Long userId);
}
