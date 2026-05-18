package com.petlife.server.modules.reminder.persistence;

import com.petlife.server.modules.reminder.persistence.command.UpdateReminderTemplateStatusCommand;
import com.petlife.server.modules.reminder.persistence.command.UpsertReminderTemplateCommand;
import com.petlife.server.modules.reminder.persistence.dataobject.ReminderTemplateDataObject;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 提醒模板持久化接口。
 */
@Mapper
public interface ReminderTemplatePersistenceMapper {

    @Select("""
        SELECT
          id AS templateId,
          template_name AS templateName,
          reminder_type AS reminderType,
          default_reminder_mode AS defaultReminderMode,
          default_advance_value AS defaultAdvanceValue,
          default_advance_unit AS defaultAdvanceUnit,
          default_cycle_value AS defaultCycleValue,
          default_cycle_unit AS defaultCycleUnit,
          applicable_pet_type AS applicablePetType,
          enabled AS enabled,
          sort_order AS sortOrder,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM reminder_templates
        WHERE deleted_at IS NULL
          AND (
            #{keyword} IS NULL
            OR template_name LIKE CONCAT('%', #{keyword}, '%')
            OR reminder_type LIKE CONCAT('%', #{keyword}, '%')
          )
          AND (#{reminderType} IS NULL OR reminder_type = #{reminderType})
          AND (#{defaultReminderMode} IS NULL OR default_reminder_mode = #{defaultReminderMode})
          AND (#{applicablePetType} IS NULL OR applicable_pet_type = #{applicablePetType})
          AND (#{enabled} IS NULL OR enabled = #{enabled})
        ORDER BY sort_order ASC, id DESC
        LIMIT 200
        """)
    List<ReminderTemplateDataObject> listAdminTemplates(
        @Param("keyword") String keyword,
        @Param("reminderType") String reminderType,
        @Param("defaultReminderMode") String defaultReminderMode,
        @Param("applicablePetType") String applicablePetType,
        @Param("enabled") Boolean enabled
    );

    @Select("""
        SELECT
          id AS templateId,
          template_name AS templateName,
          reminder_type AS reminderType,
          default_reminder_mode AS defaultReminderMode,
          default_advance_value AS defaultAdvanceValue,
          default_advance_unit AS defaultAdvanceUnit,
          default_cycle_value AS defaultCycleValue,
          default_cycle_unit AS defaultCycleUnit,
          applicable_pet_type AS applicablePetType,
          enabled AS enabled,
          sort_order AS sortOrder,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM reminder_templates
        WHERE deleted_at IS NULL
          AND enabled = 1
          AND (applicable_pet_type = 'all' OR applicable_pet_type = #{petType})
        ORDER BY sort_order ASC, id DESC
        LIMIT 100
        """)
    List<ReminderTemplateDataObject> listEnabledTemplatesForPetType(@Param("petType") String petType);

    @Select("""
        SELECT
          id AS templateId,
          template_name AS templateName,
          reminder_type AS reminderType,
          default_reminder_mode AS defaultReminderMode,
          default_advance_value AS defaultAdvanceValue,
          default_advance_unit AS defaultAdvanceUnit,
          default_cycle_value AS defaultCycleValue,
          default_cycle_unit AS defaultCycleUnit,
          applicable_pet_type AS applicablePetType,
          enabled AS enabled,
          sort_order AS sortOrder,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM reminder_templates
        WHERE id = #{templateId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    ReminderTemplateDataObject findAdminTemplateById(@Param("templateId") Long templateId);

    @Insert("""
        INSERT INTO reminder_templates (
          template_name, reminder_type, default_reminder_mode,
          default_advance_value, default_advance_unit,
          default_cycle_value, default_cycle_unit,
          applicable_pet_type, enabled, sort_order,
          created_at, updated_at
        ) VALUES (
          #{templateName}, #{reminderType}, #{defaultReminderMode},
          #{defaultAdvanceValue}, #{defaultAdvanceUnit},
          #{defaultCycleValue}, #{defaultCycleUnit},
          #{applicablePetType}, #{enabled}, #{sortOrder},
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "templateId")
    int insertTemplate(UpsertReminderTemplateCommand command);

    @Update("""
        UPDATE reminder_templates
        SET template_name = #{templateName},
            reminder_type = #{reminderType},
            default_reminder_mode = #{defaultReminderMode},
            default_advance_value = #{defaultAdvanceValue},
            default_advance_unit = #{defaultAdvanceUnit},
            default_cycle_value = #{defaultCycleValue},
            default_cycle_unit = #{defaultCycleUnit},
            applicable_pet_type = #{applicablePetType},
            enabled = #{enabled},
            sort_order = #{sortOrder},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{templateId}
          AND deleted_at IS NULL
        """)
    int updateTemplate(UpsertReminderTemplateCommand command);

    @Update("""
        UPDATE reminder_templates
        SET enabled = #{enabled},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{templateId}
          AND deleted_at IS NULL
        """)
    int updateTemplateStatus(UpdateReminderTemplateStatusCommand command);
}
