package com.petlife.server.modules.service.persistence;

import com.petlife.server.modules.service.persistence.command.CancelServiceAppointmentCommand;
import com.petlife.server.modules.service.persistence.command.CreateProviderReviewCommand;
import com.petlife.server.modules.service.persistence.command.CreateServiceAppointmentCommand;
import com.petlife.server.modules.service.persistence.command.UpdateProviderReviewStatusCommand;
import com.petlife.server.modules.service.persistence.command.UpdateServiceAppointmentStatusCommand;
import com.petlife.server.modules.service.persistence.command.UpdateServiceProviderLocationCommand;
import com.petlife.server.modules.service.persistence.command.UpsertProviderScheduleSlotCommand;
import com.petlife.server.modules.service.persistence.command.UpsertProviderServiceItemCommand;
import com.petlife.server.modules.service.persistence.command.UpsertServiceCityConfigCommand;
import com.petlife.server.modules.service.persistence.command.UpsertServiceProviderCommand;
import com.petlife.server.modules.service.persistence.dataobject.ProviderReviewDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ProviderScheduleSlotDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ProviderServiceItemDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceAppointmentDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceCategoryCountDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceCityConfigDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceProviderDataObject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 服务中心持久化 Mapper。
 */
@Mapper
public interface ServiceCenterPersistenceMapper {

    @Select("""
        SELECT
          id AS configId,
          city_code AS cityCode,
          city_name AS cityName,
          opened AS opened,
          unavailable_reason AS unavailableReason,
          sort_order AS sortOrder,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM service_city_configs
        WHERE (#{cityCode} IS NULL OR city_code = #{cityCode})
          AND (#{opened} IS NULL OR opened = #{opened})
          AND deleted_at IS NULL
        ORDER BY opened DESC, sort_order ASC, city_code ASC
        LIMIT 200
        """)
    List<ServiceCityConfigDataObject> listCityConfigs(
        @Param("cityCode") String cityCode,
        @Param("opened") Boolean opened
    );

    @Select("""
        SELECT
          id AS configId,
          city_code AS cityCode,
          city_name AS cityName,
          opened AS opened,
          unavailable_reason AS unavailableReason,
          sort_order AS sortOrder,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM service_city_configs
        WHERE city_code = #{cityCode}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    ServiceCityConfigDataObject findCityConfigByCityCode(@Param("cityCode") String cityCode);

    @Insert("""
        INSERT INTO service_city_configs (
          city_code, city_name, opened, unavailable_reason, sort_order,
          created_at, updated_at
        ) VALUES (
          #{cityCode}, #{cityName}, #{opened}, #{unavailableReason}, #{sortOrder},
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        ON DUPLICATE KEY UPDATE
          city_name = VALUES(city_name),
          opened = VALUES(opened),
          unavailable_reason = VALUES(unavailable_reason),
          sort_order = VALUES(sort_order),
          deleted_at = NULL,
          updated_at = CURRENT_TIMESTAMP
        """)
    int upsertCityConfig(UpsertServiceCityConfigCommand command);

    @Select("""
        SELECT
          provider_type AS providerType,
          COUNT(1) AS providerCount
        FROM service_providers
        WHERE city_code = #{cityCode}
          AND status = 'online'
          AND deleted_at IS NULL
        GROUP BY provider_type
        """)
    List<ServiceCategoryCountDataObject> listProviderCountsByCity(@Param("cityCode") String cityCode);

    @Select("""
        SELECT
          id AS providerId,
          provider_type AS providerType,
          provider_name AS providerName,
          city_code AS cityCode,
          address AS address,
          latitude AS latitude,
          longitude AS longitude,
          coordinate_source AS coordinateSource,
          contact_phone AS contactPhone,
          business_hours AS businessHours,
          rating_avg AS ratingAvg,
          review_count AS reviewCount,
          status AS status,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(ext_json, JSON_OBJECT()), '$')) AS extJson,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM service_providers
        WHERE (#{providerType} IS NULL OR provider_type = #{providerType})
          AND (#{cityCode} IS NULL OR city_code = #{cityCode})
          AND (#{status} IS NULL OR status = #{status})
          AND deleted_at IS NULL
        ORDER BY updated_at DESC, id DESC
        """)
    List<ServiceProviderDataObject> listAdminProviders(
        @Param("providerType") String providerType,
        @Param("cityCode") String cityCode,
        @Param("status") String status
    );

    @Select("""
        SELECT
          id AS providerId,
          provider_type AS providerType,
          provider_name AS providerName,
          city_code AS cityCode,
          address AS address,
          latitude AS latitude,
          longitude AS longitude,
          coordinate_source AS coordinateSource,
          contact_phone AS contactPhone,
          business_hours AS businessHours,
          rating_avg AS ratingAvg,
          review_count AS reviewCount,
          status AS status,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(ext_json, JSON_OBJECT()), '$')) AS extJson,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM service_providers
        WHERE city_code = #{cityCode}
          AND (#{providerType} IS NULL OR provider_type = #{providerType})
          AND status IN ('online', 'rest')
          AND deleted_at IS NULL
        ORDER BY CASE status WHEN 'online' THEN 0 ELSE 1 END ASC,
                 COALESCE(rating_avg, 0) DESC,
                 review_count DESC,
                 id ASC
        """)
    List<ServiceProviderDataObject> listProviders(
        @Param("cityCode") String cityCode,
        @Param("providerType") String providerType
    );

    @Select("""
        SELECT
          id AS providerId,
          provider_type AS providerType,
          provider_name AS providerName,
          city_code AS cityCode,
          address AS address,
          latitude AS latitude,
          longitude AS longitude,
          coordinate_source AS coordinateSource,
          contact_phone AS contactPhone,
          business_hours AS businessHours,
          rating_avg AS ratingAvg,
          review_count AS reviewCount,
          status AS status,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(ext_json, JSON_OBJECT()), '$')) AS extJson,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM service_providers
        WHERE city_code = #{cityCode}
          AND status = 'online'
          AND deleted_at IS NULL
        ORDER BY COALESCE(rating_avg, 0) DESC, review_count DESC, id ASC
        LIMIT #{limit}
        """)
    List<ServiceProviderDataObject> listFeaturedProviders(
        @Param("cityCode") String cityCode,
        @Param("limit") int limit
    );

    @Select("""
        SELECT
          id AS providerId,
          provider_type AS providerType,
          provider_name AS providerName,
          city_code AS cityCode,
          address AS address,
          latitude AS latitude,
          longitude AS longitude,
          coordinate_source AS coordinateSource,
          contact_phone AS contactPhone,
          business_hours AS businessHours,
          rating_avg AS ratingAvg,
          review_count AS reviewCount,
          status AS status,
          JSON_UNQUOTE(JSON_EXTRACT(COALESCE(ext_json, JSON_OBJECT()), '$')) AS extJson,
          created_at AS createdAt,
          updated_at AS updatedAt
        FROM service_providers
        WHERE id = #{providerId}
          AND deleted_at IS NULL
        LIMIT 1
        """)
    ServiceProviderDataObject findProviderById(@Param("providerId") Long providerId);

    @Insert("""
        INSERT INTO service_providers (
          provider_type, provider_name, city_code, address, latitude, longitude,
          coordinate_source, coordinate_updated_at,
          contact_phone, business_hours, rating_avg, review_count, status,
          created_at, updated_at
        ) VALUES (
          #{providerType}, #{providerName}, #{cityCode}, #{address}, #{latitude}, #{longitude},
          #{coordinateSource}, CASE WHEN #{latitude} IS NULL OR #{longitude} IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END,
          #{contactPhone}, #{businessHours}, #{ratingAvg}, #{reviewCount}, #{status},
          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "providerId")
    int insertProvider(UpsertServiceProviderCommand command);

    @Update("""
        UPDATE service_providers
        SET provider_type = #{providerType},
            provider_name = #{providerName},
            city_code = #{cityCode},
            address = #{address},
            latitude = #{latitude},
            longitude = #{longitude},
            coordinate_source = #{coordinateSource},
            coordinate_updated_at = CASE
              WHEN #{latitude} IS NULL OR #{longitude} IS NULL THEN NULL
              ELSE CURRENT_TIMESTAMP
            END,
            contact_phone = #{contactPhone},
            business_hours = #{businessHours},
            rating_avg = #{ratingAvg},
            review_count = #{reviewCount},
            status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{providerId}
          AND deleted_at IS NULL
        """)
    int updateProvider(UpsertServiceProviderCommand command);

    @Update("""
        UPDATE service_providers
        SET address = COALESCE(#{address}, address),
            latitude = #{latitude},
            longitude = #{longitude},
            coordinate_source = #{coordinateSource},
            coordinate_updated_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{providerId}
          AND deleted_at IS NULL
        """)
    int updateProviderLocation(UpdateServiceProviderLocationCommand command);

    @Select("""
        SELECT
          id AS serviceItemId,
          provider_id AS providerId,
          service_code AS serviceCode,
          service_name AS serviceName,
          service_desc AS serviceDesc,
          price_min AS priceMin,
          price_max AS priceMax,
          status AS status
        FROM provider_service_items
        WHERE provider_id = #{providerId}
          AND status = 'active'
        ORDER BY id ASC
        """)
    List<ProviderServiceItemDataObject> listServiceItemsByProviderId(@Param("providerId") Long providerId);

    @Select("""
        SELECT
          id AS serviceItemId,
          provider_id AS providerId,
          service_code AS serviceCode,
          service_name AS serviceName,
          service_desc AS serviceDesc,
          price_min AS priceMin,
          price_max AS priceMax,
          status AS status
        FROM provider_service_items
        WHERE provider_id = #{providerId}
        ORDER BY CASE status WHEN 'active' THEN 0 ELSE 1 END ASC, id ASC
        """)
    List<ProviderServiceItemDataObject> listAllServiceItemsByProviderId(@Param("providerId") Long providerId);

    @Select("""
        SELECT
          id AS serviceItemId,
          provider_id AS providerId,
          service_code AS serviceCode,
          service_name AS serviceName,
          service_desc AS serviceDesc,
          price_min AS priceMin,
          price_max AS priceMax,
          status AS status
        FROM provider_service_items
        WHERE id = #{serviceItemId}
          AND provider_id = #{providerId}
        LIMIT 1
        """)
    ProviderServiceItemDataObject findServiceItemById(
        @Param("providerId") Long providerId,
        @Param("serviceItemId") Long serviceItemId
    );

    @Insert("""
        INSERT INTO provider_service_items (
          provider_id, service_code, service_name, service_desc,
          price_min, price_max, status, created_at, updated_at
        ) VALUES (
          #{providerId}, #{serviceCode}, #{serviceName}, #{serviceDesc},
          #{priceMin}, #{priceMax}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "serviceItemId")
    int insertServiceItem(UpsertProviderServiceItemCommand command);

    @Update("""
        UPDATE provider_service_items
        SET service_code = #{serviceCode},
            service_name = #{serviceName},
            service_desc = #{serviceDesc},
            price_min = #{priceMin},
            price_max = #{priceMax},
            status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{serviceItemId}
          AND provider_id = #{providerId}
        """)
    int updateServiceItem(UpsertProviderServiceItemCommand command);

    @Select("""
        SELECT
          id AS slotId,
          provider_id AS providerId,
          appointment_type AS appointmentType,
          slot_date AS slotDate,
          start_time AS startTime,
          end_time AS endTime,
          quota AS quota,
          booked_count AS bookedCount,
          status AS status
        FROM provider_schedule_slots
        WHERE provider_id = #{providerId}
          AND (#{appointmentType} IS NULL OR appointment_type = #{appointmentType})
          AND slot_date BETWEEN #{startDate} AND #{endDate}
        ORDER BY slot_date ASC, start_time ASC, id ASC
        """)
    List<ProviderScheduleSlotDataObject> listScheduleSlots(
        @Param("providerId") Long providerId,
        @Param("appointmentType") String appointmentType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Select("""
        SELECT
          id AS slotId,
          provider_id AS providerId,
          appointment_type AS appointmentType,
          slot_date AS slotDate,
          start_time AS startTime,
          end_time AS endTime,
          quota AS quota,
          booked_count AS bookedCount,
          status AS status
        FROM provider_schedule_slots
        WHERE id = #{slotId}
          AND provider_id = #{providerId}
        LIMIT 1
        """)
    ProviderScheduleSlotDataObject findScheduleSlotById(
        @Param("providerId") Long providerId,
        @Param("slotId") Long slotId
    );

    @Insert("""
        INSERT INTO provider_schedule_slots (
          provider_id, appointment_type, slot_date, start_time, end_time,
          quota, booked_count, status, created_at, updated_at
        ) VALUES (
          #{providerId}, #{appointmentType}, #{slotDate}, #{startTime}, #{endTime},
          #{quota}, 0, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "slotId")
    int insertScheduleSlot(UpsertProviderScheduleSlotCommand command);

    @Update("""
        UPDATE provider_schedule_slots
        SET appointment_type = #{appointmentType},
            slot_date = #{slotDate},
            start_time = #{startTime},
            end_time = #{endTime},
            quota = #{quota},
            status = CASE
              WHEN booked_count >= #{quota} AND #{status} = 'open' THEN 'full'
              ELSE #{status}
            END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{slotId}
          AND provider_id = #{providerId}
        """)
    int updateScheduleSlot(UpsertProviderScheduleSlotCommand command);

    @Select("""
        SELECT
          id AS slotId,
          provider_id AS providerId,
          appointment_type AS appointmentType,
          slot_date AS slotDate,
          start_time AS startTime,
          end_time AS endTime,
          quota AS quota,
          booked_count AS bookedCount,
          status AS status
        FROM provider_schedule_slots
        WHERE provider_id = #{providerId}
          AND appointment_type = #{appointmentType}
          AND slot_date = #{slotDate}
          AND start_time = #{startTime}
          AND end_time = #{endTime}
        LIMIT 1
        FOR UPDATE
        """)
    ProviderScheduleSlotDataObject findScheduleSlotForUpdate(
        @Param("providerId") Long providerId,
        @Param("appointmentType") String appointmentType,
        @Param("slotDate") LocalDate slotDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    @Update("""
        UPDATE provider_schedule_slots
        SET booked_count = booked_count + 1,
            status = CASE WHEN booked_count + 1 >= quota THEN 'full' ELSE 'open' END,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{slotId}
          AND status = 'open'
          AND booked_count < quota
        """)
    int increaseScheduleSlotBookedCount(@Param("slotId") Long slotId);

    @Update("""
        UPDATE provider_schedule_slots
        SET booked_count = GREATEST(0, booked_count - 1),
            status = 'open',
            updated_at = CURRENT_TIMESTAMP
        WHERE provider_id = #{providerId}
          AND appointment_type = #{appointmentType}
          AND slot_date = #{slotDate}
          AND start_time = #{startTime}
          AND end_time = #{endTime}
          AND booked_count > 0
        """)
    int decreaseScheduleSlotBookedCount(
        @Param("providerId") Long providerId,
        @Param("appointmentType") String appointmentType,
        @Param("slotDate") LocalDate slotDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    @Insert("""
        INSERT INTO service_appointments (
          user_id, pet_id, provider_id, appointment_type, appointment_date,
          appointment_slot, demand_desc, contact_name, contact_mobile,
          status, created_at, updated_at
        ) VALUES (
          #{userId}, #{petId}, #{providerId}, #{appointmentType}, #{appointmentDate},
          #{appointmentSlot}, #{demandDesc}, #{contactName}, #{contactMobile},
          'pending_confirm', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAppointment(CreateServiceAppointmentCommand command);

    @Select("""
        SELECT
          a.id AS appointmentId,
          a.user_id AS userId,
          a.pet_id AS petId,
          p.pet_name AS petName,
          a.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          a.appointment_type AS appointmentType,
          a.appointment_date AS appointmentDate,
          a.appointment_slot AS appointmentSlot,
          a.demand_desc AS demandDesc,
          a.contact_name AS contactName,
          a.contact_mobile AS contactMobile,
          a.status AS status,
          a.remark AS remark,
          EXISTS (
            SELECT 1
            FROM provider_reviews pr
            WHERE pr.appointment_id = a.id
              AND pr.deleted_at IS NULL
          ) AS reviewed,
          a.created_at AS createdAt,
          a.updated_at AS updatedAt
        FROM service_appointments a
        JOIN pets p ON p.id = a.pet_id
        JOIN service_providers sp ON sp.id = a.provider_id
        WHERE a.id = #{appointmentId}
          AND a.deleted_at IS NULL
        LIMIT 1
        """)
    ServiceAppointmentDataObject findAppointmentById(@Param("appointmentId") Long appointmentId);

    @Select("""
        SELECT
          a.id AS appointmentId,
          a.user_id AS userId,
          a.pet_id AS petId,
          p.pet_name AS petName,
          a.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          a.appointment_type AS appointmentType,
          a.appointment_date AS appointmentDate,
          a.appointment_slot AS appointmentSlot,
          a.demand_desc AS demandDesc,
          a.contact_name AS contactName,
          a.contact_mobile AS contactMobile,
          a.status AS status,
          a.remark AS remark,
          EXISTS (
            SELECT 1
            FROM provider_reviews pr
            WHERE pr.appointment_id = a.id
              AND pr.deleted_at IS NULL
          ) AS reviewed,
          a.created_at AS createdAt,
          a.updated_at AS updatedAt
        FROM service_appointments a
        JOIN pets p ON p.id = a.pet_id
        JOIN service_providers sp ON sp.id = a.provider_id
        WHERE a.id = #{appointmentId}
          AND a.user_id = #{userId}
          AND a.deleted_at IS NULL
        LIMIT 1
        """)
    ServiceAppointmentDataObject findAppointmentByUserIdAndId(
        @Param("userId") Long userId,
        @Param("appointmentId") Long appointmentId
    );

    @Select("""
        SELECT
          a.id AS appointmentId,
          a.user_id AS userId,
          a.pet_id AS petId,
          p.pet_name AS petName,
          a.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          a.appointment_type AS appointmentType,
          a.appointment_date AS appointmentDate,
          a.appointment_slot AS appointmentSlot,
          a.demand_desc AS demandDesc,
          a.contact_name AS contactName,
          a.contact_mobile AS contactMobile,
          a.status AS status,
          a.remark AS remark,
          EXISTS (
            SELECT 1
            FROM provider_reviews pr
            WHERE pr.appointment_id = a.id
              AND pr.deleted_at IS NULL
          ) AS reviewed,
          a.created_at AS createdAt,
          a.updated_at AS updatedAt
        FROM service_appointments a
        JOIN pets p ON p.id = a.pet_id
        JOIN service_providers sp ON sp.id = a.provider_id
        WHERE a.user_id = #{userId}
          AND (#{status} IS NULL OR a.status = #{status})
          AND a.deleted_at IS NULL
        ORDER BY a.appointment_date DESC, a.id DESC
        LIMIT 100
        """)
    List<ServiceAppointmentDataObject> listAppointmentsByUserId(
        @Param("userId") Long userId,
        @Param("status") String status
    );

    @Select("""
        SELECT
          a.id AS appointmentId,
          a.user_id AS userId,
          a.pet_id AS petId,
          p.pet_name AS petName,
          a.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          a.appointment_type AS appointmentType,
          a.appointment_date AS appointmentDate,
          a.appointment_slot AS appointmentSlot,
          a.demand_desc AS demandDesc,
          a.contact_name AS contactName,
          a.contact_mobile AS contactMobile,
          a.status AS status,
          a.remark AS remark,
          EXISTS (
            SELECT 1
            FROM provider_reviews pr
            WHERE pr.appointment_id = a.id
              AND pr.deleted_at IS NULL
          ) AS reviewed,
          a.created_at AS createdAt,
          a.updated_at AS updatedAt
        FROM service_appointments a
        JOIN pets p ON p.id = a.pet_id
        JOIN service_providers sp ON sp.id = a.provider_id
        WHERE (#{status} IS NULL OR a.status = #{status})
          AND (#{providerType} IS NULL OR a.appointment_type = #{providerType})
          AND (#{cityCode} IS NULL OR sp.city_code = #{cityCode})
          AND a.deleted_at IS NULL
        ORDER BY a.appointment_date DESC, a.id DESC
        LIMIT 200
        """)
    List<ServiceAppointmentDataObject> listAdminAppointments(
        @Param("status") String status,
        @Param("providerType") String providerType,
        @Param("cityCode") String cityCode
    );

    @Select("""
        SELECT
          a.id AS appointmentId,
          a.user_id AS userId,
          a.pet_id AS petId,
          p.pet_name AS petName,
          a.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          a.appointment_type AS appointmentType,
          a.appointment_date AS appointmentDate,
          a.appointment_slot AS appointmentSlot,
          a.demand_desc AS demandDesc,
          a.contact_name AS contactName,
          a.contact_mobile AS contactMobile,
          a.status AS status,
          a.remark AS remark,
          EXISTS (
            SELECT 1
            FROM provider_reviews pr
            WHERE pr.appointment_id = a.id
              AND pr.deleted_at IS NULL
          ) AS reviewed,
          a.created_at AS createdAt,
          a.updated_at AS updatedAt
        FROM service_appointments a
        JOIN pets p ON p.id = a.pet_id
        JOIN service_providers sp ON sp.id = a.provider_id
        WHERE a.user_id = #{userId}
          AND a.status IN ('pending_confirm', 'confirmed')
          AND a.appointment_date >= CURRENT_DATE
          AND a.deleted_at IS NULL
        ORDER BY a.appointment_date ASC, a.id ASC
        LIMIT #{limit}
        """)
    List<ServiceAppointmentDataObject> listUpcomingAppointmentsByUserId(
        @Param("userId") Long userId,
        @Param("limit") int limit
    );

    @Update("""
        UPDATE service_appointments
        SET status = 'canceled',
            remark = #{cancelReason},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{appointmentId}
          AND user_id = #{userId}
          AND status IN ('pending_confirm', 'confirmed')
          AND deleted_at IS NULL
        """)
    int cancelAppointment(CancelServiceAppointmentCommand command);

    @Update("""
        UPDATE service_appointments
        SET status = #{status},
            remark = #{remark},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{appointmentId}
          AND deleted_at IS NULL
        """)
    int updateAppointmentStatus(UpdateServiceAppointmentStatusCommand command);

    @Select("""
        SELECT
          r.id AS reviewId,
          r.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          r.appointment_id AS appointmentId,
          r.user_id AS userId,
          u.nickname AS reviewerNickname,
          r.pet_id AS petId,
          p.pet_name AS petName,
          r.rating AS rating,
          r.content AS content,
          r.status AS status,
          r.created_at AS createdAt,
          r.updated_at AS updatedAt
        FROM provider_reviews r
        JOIN service_providers sp ON sp.id = r.provider_id
        JOIN users u ON u.id = r.user_id
        LEFT JOIN pets p ON p.id = r.pet_id
        WHERE r.id = #{reviewId}
          AND r.deleted_at IS NULL
        LIMIT 1
        """)
    ProviderReviewDataObject findReviewById(@Param("reviewId") Long reviewId);

    @Select("""
        SELECT
          r.id AS reviewId,
          r.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          r.appointment_id AS appointmentId,
          r.user_id AS userId,
          u.nickname AS reviewerNickname,
          r.pet_id AS petId,
          p.pet_name AS petName,
          r.rating AS rating,
          r.content AS content,
          r.status AS status,
          r.created_at AS createdAt,
          r.updated_at AS updatedAt
        FROM provider_reviews r
        JOIN service_providers sp ON sp.id = r.provider_id
        JOIN users u ON u.id = r.user_id
        LEFT JOIN pets p ON p.id = r.pet_id
        WHERE r.appointment_id = #{appointmentId}
          AND r.deleted_at IS NULL
        LIMIT 1
        """)
    ProviderReviewDataObject findReviewByAppointmentId(@Param("appointmentId") Long appointmentId);

    @Select("""
        SELECT
          r.id AS reviewId,
          r.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          r.appointment_id AS appointmentId,
          r.user_id AS userId,
          u.nickname AS reviewerNickname,
          r.pet_id AS petId,
          p.pet_name AS petName,
          r.rating AS rating,
          r.content AS content,
          r.status AS status,
          r.created_at AS createdAt,
          r.updated_at AS updatedAt
        FROM provider_reviews r
        JOIN service_providers sp ON sp.id = r.provider_id
        JOIN users u ON u.id = r.user_id
        LEFT JOIN pets p ON p.id = r.pet_id
        WHERE r.provider_id = #{providerId}
          AND r.status = 'visible'
          AND r.deleted_at IS NULL
        ORDER BY r.created_at DESC, r.id DESC
        LIMIT 50
        """)
    List<ProviderReviewDataObject> listVisibleReviewsByProviderId(@Param("providerId") Long providerId);

    @Select("""
        SELECT
          r.id AS reviewId,
          r.provider_id AS providerId,
          sp.provider_name AS providerName,
          sp.provider_type AS providerType,
          r.appointment_id AS appointmentId,
          r.user_id AS userId,
          u.nickname AS reviewerNickname,
          r.pet_id AS petId,
          p.pet_name AS petName,
          r.rating AS rating,
          r.content AS content,
          r.status AS status,
          r.created_at AS createdAt,
          r.updated_at AS updatedAt
        FROM provider_reviews r
        JOIN service_providers sp ON sp.id = r.provider_id
        JOIN users u ON u.id = r.user_id
        LEFT JOIN pets p ON p.id = r.pet_id
        WHERE (#{status} IS NULL OR r.status = #{status})
          AND (#{providerType} IS NULL OR sp.provider_type = #{providerType})
          AND (#{cityCode} IS NULL OR sp.city_code = #{cityCode})
          AND r.deleted_at IS NULL
        ORDER BY r.created_at DESC, r.id DESC
        LIMIT 200
        """)
    List<ProviderReviewDataObject> listAdminReviews(
        @Param("status") String status,
        @Param("providerType") String providerType,
        @Param("cityCode") String cityCode
    );

    @Insert("""
        INSERT INTO provider_reviews (
          provider_id, appointment_id, user_id, pet_id, rating, content,
          status, created_at, updated_at
        ) VALUES (
          #{providerId}, #{appointmentId}, #{userId}, #{petId}, #{rating}, #{content},
          'visible', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """)
    @Options(useGeneratedKeys = true, keyProperty = "reviewId")
    int insertReview(CreateProviderReviewCommand command);

    @Update("""
        UPDATE provider_reviews
        SET status = #{status},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{reviewId}
          AND deleted_at IS NULL
        """)
    int updateReviewStatus(UpdateProviderReviewStatusCommand command);

    @Update("""
        UPDATE service_providers sp
        SET rating_avg = (
              SELECT ROUND(AVG(r.rating), 2)
              FROM provider_reviews r
              WHERE r.provider_id = sp.id
                AND r.status = 'visible'
                AND r.deleted_at IS NULL
            ),
            review_count = (
              SELECT COUNT(1)
              FROM provider_reviews r
              WHERE r.provider_id = sp.id
                AND r.status = 'visible'
                AND r.deleted_at IS NULL
            ),
            updated_at = CURRENT_TIMESTAMP
        WHERE sp.id = #{providerId}
          AND sp.deleted_at IS NULL
        """)
    int refreshProviderReviewSummary(@Param("providerId") Long providerId);
}
