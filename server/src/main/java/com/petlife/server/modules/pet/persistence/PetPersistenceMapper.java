package com.petlife.server.modules.pet.persistence;

import com.petlife.server.modules.pet.persistence.command.CreatePetCommand;
import com.petlife.server.modules.pet.persistence.dataobject.PetProfileDataObject;
import java.time.LocalDate;
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

    @Insert("""
        INSERT INTO pets (
          family_id, owner_user_id, pet_name, pet_type, breed, gender, birthday, adopt_date,
          neuter_status, avatar_url, status, created_at, updated_at
        ) VALUES (
          #{familyId}, #{ownerUserId}, #{petName}, #{petType}, #{breed}, #{gender}, #{birthday}, #{adoptDate},
          #{neuterStatus}, #{avatarUrl}, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
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
    int updatePetSnapshot(
        @Param("petId") Long petId,
        @Param("userId") Long userId,
        @Param("petName") String petName,
        @Param("petType") String petType,
        @Param("breed") String breed,
        @Param("gender") String gender,
        @Param("birthday") LocalDate birthday,
        @Param("adoptDate") LocalDate adoptDate,
        @Param("neuterStatus") Integer neuterStatus,
        @Param("avatarUrl") String avatarUrl
    );
}
