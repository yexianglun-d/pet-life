package com.petlife.server.modules.media.persistence;

import com.petlife.server.modules.media.persistence.command.CreateMediaAssetCommand;
import com.petlife.server.modules.media.persistence.dataobject.MediaAssetDataObject;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 媒体资产持久化 Mapper。
 */
@Mapper
public interface MediaAssetPersistenceMapper {

    @Insert("""
        INSERT INTO media_assets (
          uploader_user_id, biz_type, media_type, file_name, object_key, bucket_name,
          cdn_url, content_type, file_size, file_hash, upload_status, review_status,
          completed_at, created_at, updated_at
        ) VALUES (
          #{uploaderUserId}, #{bizType}, #{mediaType}, #{fileName}, #{objectKey}, #{bucketName},
          #{cdnUrl}, #{contentType}, #{fileSize}, #{fileHash}, #{uploadStatus}, #{reviewStatus},
          #{completedAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMediaAsset(CreateMediaAssetCommand command);

    @Select("""
        SELECT
          id AS mediaAssetId,
          uploader_user_id AS uploaderUserId,
          biz_type AS bizType,
          media_type AS mediaType,
          file_name AS fileName,
          object_key AS objectKey,
          bucket_name AS bucketName,
          cdn_url AS cdnUrl,
          content_type AS contentType,
          file_size AS fileSize,
          file_hash AS fileHash,
          upload_status AS uploadStatus,
          review_status AS reviewStatus,
          completed_at AS completedAt,
          created_at AS createdAt
        FROM media_assets
        WHERE id = #{mediaAssetId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    MediaAssetDataObject findMediaAssetById(@Param("mediaAssetId") Long mediaAssetId);

    @Select("""
        SELECT
          id AS mediaAssetId,
          uploader_user_id AS uploaderUserId,
          biz_type AS bizType,
          media_type AS mediaType,
          file_name AS fileName,
          object_key AS objectKey,
          bucket_name AS bucketName,
          cdn_url AS cdnUrl,
          content_type AS contentType,
          file_size AS fileSize,
          file_hash AS fileHash,
          upload_status AS uploadStatus,
          review_status AS reviewStatus,
          completed_at AS completedAt,
          created_at AS createdAt
        FROM media_assets
        WHERE id = #{mediaAssetId}
          AND upload_status = 'uploaded'
          AND deleted_at IS NULL
        LIMIT 1
        """)
    MediaAssetDataObject findUploadedAssetById(@Param("mediaAssetId") Long mediaAssetId);

    @Select("""
        SELECT
          id AS mediaAssetId,
          uploader_user_id AS uploaderUserId,
          biz_type AS bizType,
          media_type AS mediaType,
          file_name AS fileName,
          object_key AS objectKey,
          bucket_name AS bucketName,
          cdn_url AS cdnUrl,
          content_type AS contentType,
          file_size AS fileSize,
          file_hash AS fileHash,
          upload_status AS uploadStatus,
          review_status AS reviewStatus,
          completed_at AS completedAt,
          created_at AS createdAt
        FROM media_assets
        WHERE id = #{mediaAssetId}
          AND uploader_user_id = #{userId}
          AND upload_status = 'uploaded'
          AND deleted_at IS NULL
        LIMIT 1
        """)
    MediaAssetDataObject findUploadedAssetByUserIdAndId(
        @Param("userId") Long userId,
        @Param("mediaAssetId") Long mediaAssetId
    );

    @Select("""
        SELECT
          m.id AS mediaAssetId,
          m.uploader_user_id AS uploaderUserId,
          m.biz_type AS bizType,
          m.media_type AS mediaType,
          m.file_name AS fileName,
          m.object_key AS objectKey,
          m.bucket_name AS bucketName,
          m.cdn_url AS cdnUrl,
          m.content_type AS contentType,
          m.file_size AS fileSize,
          m.file_hash AS fileHash,
          m.upload_status AS uploadStatus,
          m.review_status AS reviewStatus,
          m.completed_at AS completedAt,
          m.created_at AS createdAt
        FROM media_assets m
        WHERE m.id = #{mediaAssetId}
          AND m.upload_status = 'uploaded'
          AND m.deleted_at IS NULL
          AND (
            m.uploader_user_id = #{userId}
            OR EXISTS (
              SELECT 1
              FROM pet_health_records h
              JOIN pets p ON p.id = h.pet_id
              WHERE h.deleted_at IS NULL
                AND JSON_CONTAINS(COALESCE(h.attachments, JSON_ARRAY()), JSON_QUOTE(CAST(m.id AS CHAR)), '$')
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
            )
            OR EXISTS (
              SELECT 1
              FROM pet_daily_logs d
              JOIN pets p ON p.id = d.pet_id
              WHERE d.deleted_at IS NULL
                AND JSON_CONTAINS(COALESCE(d.media_list, JSON_ARRAY()), JSON_QUOTE(CAST(m.id AS CHAR)), '$')
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
            )
          )
        LIMIT 1
        """)
    MediaAssetDataObject findReadableUploadedAssetByUserIdAndId(
        @Param("userId") Long userId,
        @Param("mediaAssetId") Long mediaAssetId
    );
}
