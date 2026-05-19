package com.petlife.server.modules.notification.persistence;

import com.petlife.server.modules.notification.persistence.command.UpdateMessageTemplateStatusCommand;
import com.petlife.server.modules.notification.persistence.command.UpsertMessageTemplateCommand;
import com.petlife.server.modules.notification.persistence.dataobject.MessageTemplateDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 消息模板持久化接口。
 */
@Mapper
public interface MessageTemplatePersistenceMapper {

    @Select("""
        SELECT
          id AS templateId,
          template_code AS templateCode,
          channel_type AS channelType,
          title_template AS titleTemplate,
          content_template AS contentTemplate,
          status AS status,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM message_templates
        WHERE (#{keyword} IS NULL
               OR template_code LIKE CONCAT('%', #{keyword}, '%')
               OR title_template LIKE CONCAT('%', #{keyword}, '%'))
          AND (#{templateCode} IS NULL OR template_code = #{templateCode})
          AND (#{channelType} IS NULL OR channel_type = #{channelType})
          AND (#{status} IS NULL OR status = #{status})
        ORDER BY updated_at DESC, id DESC
        LIMIT 200
        """)
    List<MessageTemplateDataObject> listTemplates(
        @Param("keyword") String keyword,
        @Param("templateCode") String templateCode,
        @Param("channelType") String channelType,
        @Param("status") String status
    );

    @Select("""
        SELECT
          id AS templateId,
          template_code AS templateCode,
          channel_type AS channelType,
          title_template AS titleTemplate,
          content_template AS contentTemplate,
          status AS status,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM message_templates
        WHERE id = #{templateId}
        LIMIT 1
        """)
    MessageTemplateDataObject findTemplateById(@Param("templateId") Long templateId);

    @Select("""
        SELECT
          id AS templateId,
          template_code AS templateCode,
          channel_type AS channelType,
          title_template AS titleTemplate,
          content_template AS contentTemplate,
          status AS status,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM message_templates
        WHERE template_code = #{templateCode}
          AND channel_type = #{channelType}
        LIMIT 1
        """)
    MessageTemplateDataObject findTemplateByCodeAndChannel(
        @Param("templateCode") String templateCode,
        @Param("channelType") String channelType
    );

    @Select("""
        SELECT
          id AS templateId,
          template_code AS templateCode,
          channel_type AS channelType,
          title_template AS titleTemplate,
          content_template AS contentTemplate,
          status AS status,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM message_templates
        WHERE template_code = #{templateCode}
          AND channel_type = #{channelType}
          AND status = 'active'
        LIMIT 1
        """)
    MessageTemplateDataObject findActiveTemplateByCodeAndChannel(
        @Param("templateCode") String templateCode,
        @Param("channelType") String channelType
    );

    @Insert("""
        INSERT INTO message_templates (
          template_code, channel_type, title_template, content_template,
          status, created_at, updated_at
        ) VALUES (
          #{templateCode}, #{channelType}, #{titleTemplate}, #{contentTemplate},
          #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "templateId")
    int insertTemplate(UpsertMessageTemplateCommand command);

    @Update("""
        UPDATE message_templates
        SET template_code = #{templateCode},
            channel_type = #{channelType},
            title_template = #{titleTemplate},
            content_template = #{contentTemplate},
            status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{templateId}
        """)
    int updateTemplate(UpsertMessageTemplateCommand command);

    @Update("""
        UPDATE message_templates
        SET status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{templateId}
        """)
    int updateTemplateStatus(UpdateMessageTemplateStatusCommand command);
}
