package com.petlife.server.modules.service.service;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.domain.entity.AdminOperationContext;
import com.petlife.server.modules.admin.service.AuditLogApplicationService;
import com.petlife.server.modules.auth.security.CurrentUserContext;
import com.petlife.server.modules.notification.service.NotificationApplicationService;
import com.petlife.server.modules.pet.persistence.PetPersistenceMapper;
import com.petlife.server.modules.service.converter.ServiceProviderConverter;
import com.petlife.server.modules.service.domain.entity.ProviderReviewEntity;
import com.petlife.server.modules.service.domain.entity.ProviderScheduleSlotEntity;
import com.petlife.server.modules.service.domain.entity.ProviderServiceItemEntity;
import com.petlife.server.modules.service.domain.entity.ServiceAppointmentEntity;
import com.petlife.server.modules.service.domain.entity.ServiceCityConfigEntity;
import com.petlife.server.modules.service.domain.entity.ServiceProviderEntity;
import com.petlife.server.modules.service.dto.request.AdminUpdateProviderReviewStatusRequest;
import com.petlife.server.modules.service.dto.request.AdminUpdateServiceAppointmentStatusRequest;
import com.petlife.server.modules.service.dto.request.AdminUpsertServiceCityConfigRequest;
import com.petlife.server.modules.service.dto.request.AdminUpsertProviderScheduleSlotRequest;
import com.petlife.server.modules.service.dto.request.AdminUpsertProviderServiceItemRequest;
import com.petlife.server.modules.service.dto.request.AdminUpsertServiceProviderRequest;
import com.petlife.server.modules.service.dto.request.CancelServiceAppointmentRequest;
import com.petlife.server.modules.service.dto.request.CreateProviderReviewRequest;
import com.petlife.server.modules.service.dto.request.CreateServiceAppointmentRequest;
import com.petlife.server.modules.service.dto.response.ProviderReviewResponse;
import com.petlife.server.modules.service.dto.response.ProviderScheduleSlotResponse;
import com.petlife.server.modules.service.dto.response.ServiceAppointmentResponse;
import com.petlife.server.modules.service.dto.response.ServiceCategoryResponse;
import com.petlife.server.modules.service.dto.response.ServiceCityConfigResponse;
import com.petlife.server.modules.service.dto.response.ServiceHomeResponse;
import com.petlife.server.modules.service.dto.response.ServiceProviderResponse;
import com.petlife.server.modules.service.persistence.ServiceCenterPersistenceMapper;
import com.petlife.server.modules.service.persistence.command.CancelServiceAppointmentCommand;
import com.petlife.server.modules.service.persistence.command.CreateProviderReviewCommand;
import com.petlife.server.modules.service.persistence.command.CreateServiceAppointmentCommand;
import com.petlife.server.modules.service.persistence.command.UpdateProviderReviewStatusCommand;
import com.petlife.server.modules.service.persistence.command.UpdateServiceAppointmentStatusCommand;
import com.petlife.server.modules.service.persistence.command.UpsertProviderScheduleSlotCommand;
import com.petlife.server.modules.service.persistence.command.UpsertProviderServiceItemCommand;
import com.petlife.server.modules.service.persistence.command.UpsertServiceCityConfigCommand;
import com.petlife.server.modules.service.persistence.command.UpsertServiceProviderCommand;
import com.petlife.server.modules.service.persistence.dataobject.ProviderScheduleSlotDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceCategoryCountDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceProviderDataObject;
import com.petlife.server.modules.timeline.service.TimelineApplicationService;
import com.petlife.server.modules.user.persistence.UserPersistenceMapper;
import com.petlife.server.modules.user.persistence.dataobject.UserSettingsDataObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 服务中心应用服务。
 *
 * <p>服务中心当前只处理本地照护预约，不承接商城交易，也不对接外部履约系统。
 * 预约创建仍然必须在服务端完成宠物权限、服务商状态和时段名额校验，避免前端展示态造成超卖。</p>
 */
@Service
public class ServiceCenterApplicationService {

    private static final Set<String> SUPPORTED_PROVIDER_TYPES = Set.of(
        "hospital",
        "boarding",
        "grooming",
        "training"
    );
    private static final Set<String> SUPPORTED_APPOINTMENT_STATUSES = Set.of(
        "pending_confirm",
        "confirmed",
        "completed",
        "canceled"
    );
    private static final Set<String> SUPPORTED_PROVIDER_STATUSES = Set.of("online", "rest", "offline");
    private static final Set<String> SUPPORTED_SERVICE_ITEM_STATUSES = Set.of("active", "inactive");
    private static final Set<String> SUPPORTED_SLOT_STATUSES = Set.of("open", "closed", "full");
    private static final Set<String> SUPPORTED_REVIEW_STATUSES = Set.of("visible", "hidden");
    private static final Map<String, ServiceCategoryDescriptor> CATEGORY_DESCRIPTORS = Map.of(
        "hospital", new ServiceCategoryDescriptor("宠物医院", "体检、疫苗、复诊和异常就医预约。"),
        "boarding", new ServiceCategoryDescriptor("寄养照看", "出行前安排寄养、接送和喂养说明。"),
        "grooming", new ServiceCategoryDescriptor("洗护美容", "洗澡、美容、护理和周期回访安排。"),
        "training", new ServiceCategoryDescriptor("训练服务", "行为训练、习惯养成和训练反馈记录。")
    );
    private static final DateTimeFormatter SLOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String DEFAULT_CITY_CODE = "310000";
    private static final String DEFAULT_CITY_NAME = "上海";
    private static final String DEFAULT_UNAVAILABLE_REASON = "当前城市服务正在准备中";
    private static final String AUDIT_TARGET_SERVICE_CITY = "service_city";
    private static final String AUDIT_TARGET_SERVICE_PROVIDER = "service_provider";
    private static final String AUDIT_TARGET_PROVIDER_SERVICE_ITEM = "provider_service_item";
    private static final String AUDIT_TARGET_PROVIDER_SCHEDULE_SLOT = "provider_schedule_slot";
    private static final String AUDIT_TARGET_SERVICE_APPOINTMENT = "service_appointment";
    private static final String AUDIT_TARGET_PROVIDER_REVIEW = "provider_review";
    private static final String AUDIT_ACTION_SERVICE_CITY_UPSERT = "service_city_upsert";
    private static final String AUDIT_ACTION_SERVICE_PROVIDER_CREATE = "service_provider_create";
    private static final String AUDIT_ACTION_SERVICE_PROVIDER_UPDATE = "service_provider_update";
    private static final String AUDIT_ACTION_PROVIDER_SERVICE_ITEM_CREATE = "provider_service_item_create";
    private static final String AUDIT_ACTION_PROVIDER_SERVICE_ITEM_UPDATE = "provider_service_item_update";
    private static final String AUDIT_ACTION_PROVIDER_SCHEDULE_SLOT_CREATE = "provider_schedule_slot_create";
    private static final String AUDIT_ACTION_PROVIDER_SCHEDULE_SLOT_UPDATE = "provider_schedule_slot_update";
    private static final String AUDIT_ACTION_SERVICE_APPOINTMENT_STATUS_UPDATE = "service_appointment_status_update";
    private static final String AUDIT_ACTION_PROVIDER_REVIEW_STATUS_UPDATE = "provider_review_status_update";

    private final ServiceCenterPersistenceMapper serviceCenterPersistenceMapper;
    private final PetPersistenceMapper petPersistenceMapper;
    private final UserPersistenceMapper userPersistenceMapper;
    private final ServiceProviderConverter serviceProviderConverter;
    private final TimelineApplicationService timelineApplicationService;
    private final NotificationApplicationService notificationApplicationService;
    private final AuditLogApplicationService auditLogApplicationService;

    public ServiceCenterApplicationService(
        ServiceCenterPersistenceMapper serviceCenterPersistenceMapper,
        PetPersistenceMapper petPersistenceMapper,
        UserPersistenceMapper userPersistenceMapper,
        ServiceProviderConverter serviceProviderConverter,
        TimelineApplicationService timelineApplicationService,
        NotificationApplicationService notificationApplicationService,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.serviceCenterPersistenceMapper = serviceCenterPersistenceMapper;
        this.petPersistenceMapper = petPersistenceMapper;
        this.userPersistenceMapper = userPersistenceMapper;
        this.serviceProviderConverter = serviceProviderConverter;
        this.timelineApplicationService = timelineApplicationService;
        this.notificationApplicationService = notificationApplicationService;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public ServiceHomeResponse getServiceHome(Long petId, String cityCode) {
        Long currentUserId = CurrentUserContext.requireUserId();
        if (petId != null) {
            requireAccessiblePet(currentUserId, petId);
        }

        CityContext cityContext = resolveCityContext(currentUserId, cityCode);
        ServiceCityConfigEntity cityConfig = resolveCityConfig(cityContext);
        List<ServiceCategoryCountDataObject> categoryCounts = cityConfig.isOpened()
            ? serviceCenterPersistenceMapper.listProviderCountsByCity(cityConfig.getCityCode())
            : List.of();
        Map<String, Integer> countMap = categoryCounts.stream()
            .collect(Collectors.toMap(ServiceCategoryCountDataObject::providerType, ServiceCategoryCountDataObject::providerCount));
        List<ServiceCategoryResponse> categories = SUPPORTED_PROVIDER_TYPES.stream()
            .map(providerType -> buildCategoryResponse(providerType, countMap.getOrDefault(providerType, 0), cityConfig.isOpened()))
            .toList();

        return new ServiceHomeResponse(
            cityConfig.getCityCode(),
            cityConfig.getCityName(),
            cityConfig.isOpened(),
            cityConfig.isOpened() ? null : resolveUnavailableReason(cityConfig),
            categories,
            cityConfig.isOpened()
                ? toProviderResponses(serviceCenterPersistenceMapper.listFeaturedProviders(cityConfig.getCityCode(), 6))
                : List.of(),
            serviceCenterPersistenceMapper.listUpcomingAppointmentsByUserId(currentUserId, 5).stream()
                .map(serviceProviderConverter::toAppointmentEntity)
                .map(serviceProviderConverter::toAppointmentResponse)
                .toList(),
            "商城当前保持预留，不进入服务预约链路"
        );
    }

    public List<ServiceProviderResponse> listProviders(String providerType, String cityCode) {
        Long currentUserId = CurrentUserContext.requireUserId();
        CityContext cityContext = resolveCityContext(currentUserId, cityCode);
        ServiceCityConfigEntity cityConfig = resolveCityConfig(cityContext);
        if (!cityConfig.isOpened()) {
            return List.of();
        }
        String normalizedProviderType = normalizeNullableProviderType(providerType);
        return toProviderResponses(serviceCenterPersistenceMapper.listProviders(cityConfig.getCityCode(), normalizedProviderType));
    }

    public ServiceProviderResponse getProviderDetail(Long providerId) {
        ServiceProviderDataObject provider = requireProvider(providerId);
        requireOpenedCity(provider.cityCode());
        return serviceProviderConverter.toProviderResponse(buildProviderEntity(provider, null, 14));
    }

    public List<ProviderScheduleSlotResponse> listProviderSlots(
        Long providerId,
        String appointmentType,
        LocalDate startDate,
        LocalDate endDate
    ) {
        ServiceProviderDataObject provider = requireProvider(providerId);
        requireOpenedCity(provider.cityCode());
        String normalizedAppointmentType = normalizeAppointmentType(
            appointmentType == null || appointmentType.isBlank() ? provider.providerType() : appointmentType
        );
        LocalDate normalizedStartDate = startDate == null ? LocalDate.now() : startDate;
        LocalDate normalizedEndDate = endDate == null ? normalizedStartDate.plusDays(13) : endDate;
        if (normalizedEndDate.isBefore(normalizedStartDate)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "结束日期不能早于开始日期");
        }
        return serviceCenterPersistenceMapper
            .listScheduleSlots(providerId, normalizedAppointmentType, normalizedStartDate, normalizedEndDate)
            .stream()
            .map(serviceProviderConverter::toScheduleSlotEntity)
            .map(serviceProviderConverter::toScheduleSlotResponse)
            .toList();
    }

    public List<ProviderReviewResponse> listProviderReviews(Long providerId) {
        requireProvider(providerId);
        return serviceCenterPersistenceMapper.listVisibleReviewsByProviderId(providerId).stream()
            .map(serviceProviderConverter::toReviewEntity)
            .map(serviceProviderConverter::toReviewResponse)
            .toList();
    }

    public List<ServiceAppointmentResponse> listAppointments(String status) {
        Long currentUserId = CurrentUserContext.requireUserId();
        String normalizedStatus = normalizeNullableAppointmentStatus(status);
        return serviceCenterPersistenceMapper.listAppointmentsByUserId(currentUserId, normalizedStatus).stream()
            .map(serviceProviderConverter::toAppointmentEntity)
            .map(serviceProviderConverter::toAppointmentResponse)
            .toList();
    }

    public List<ServiceProviderResponse> listAdminProviders(
        String providerType,
        String cityCode,
        String status
    ) {
        String normalizedProviderType = normalizeNullableProviderType(providerType);
        String normalizedCityCode = normalizeNullableText(cityCode);
        String normalizedStatus = normalizeNullableProviderStatus(status);
        return serviceCenterPersistenceMapper
            .listAdminProviders(normalizedProviderType, normalizedCityCode, normalizedStatus)
            .stream()
            .map(this::buildAdminProviderEntity)
            .map(serviceProviderConverter::toProviderResponse)
            .toList();
    }

    public List<ServiceCityConfigResponse> listAdminCityConfigs(String cityCode, Boolean opened) {
        String normalizedCityCode = normalizeNullableText(cityCode);
        return serviceCenterPersistenceMapper
            .listCityConfigs(normalizedCityCode, opened)
            .stream()
            .map(serviceProviderConverter::toCityConfigEntity)
            .map(serviceProviderConverter::toCityConfigResponse)
            .toList();
    }

    @Transactional
    public ServiceCityConfigResponse upsertAdminCityConfig(
        AdminUpsertServiceCityConfigRequest request,
        AdminOperationContext operationContext
    ) {
        UpsertServiceCityConfigCommand command = buildCityConfigCommand(request);
        serviceCenterPersistenceMapper.upsertCityConfig(command);
        ServiceCityConfigEntity cityConfig = serviceProviderConverter.toCityConfigEntity(
            serviceCenterPersistenceMapper.findCityConfigByCityCode(command.getCityCode())
        );
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_SERVICE_CITY,
            cityConfig.getCityCode(),
            AUDIT_ACTION_SERVICE_CITY_UPSERT,
            auditDetail(
                "city_code", cityConfig.getCityCode(),
                "city_name", cityConfig.getCityName(),
                "opened", cityConfig.isOpened(),
                "unavailable_reason", cityConfig.getUnavailableReason()
            )
        );
        return serviceProviderConverter.toCityConfigResponse(cityConfig);
    }

    @Transactional
    public ServiceProviderResponse createAdminProvider(
        AdminUpsertServiceProviderRequest request,
        AdminOperationContext operationContext
    ) {
        UpsertServiceProviderCommand command = buildProviderCommand(null, request);
        serviceCenterPersistenceMapper.insertProvider(command);
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_SERVICE_PROVIDER,
            command.getProviderId().toString(),
            AUDIT_ACTION_SERVICE_PROVIDER_CREATE,
            auditDetail(
                "provider_type", command.getProviderType(),
                "provider_name", command.getProviderName(),
                "city_code", command.getCityCode(),
                "status", command.getStatus()
            )
        );
        return serviceProviderConverter.toProviderResponse(buildAdminProviderEntity(requireProvider(command.getProviderId())));
    }

    @Transactional
    public ServiceProviderResponse updateAdminProvider(
        Long providerId,
        AdminUpsertServiceProviderRequest request,
        AdminOperationContext operationContext
    ) {
        requireProvider(providerId);
        UpsertServiceProviderCommand command = buildProviderCommand(providerId, request);
        int updatedRows = serviceCenterPersistenceMapper.updateProvider(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.SERVICE_PROVIDER_NOT_FOUND);
        }
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_SERVICE_PROVIDER,
            providerId.toString(),
            AUDIT_ACTION_SERVICE_PROVIDER_UPDATE,
            auditDetail(
                "provider_type", command.getProviderType(),
                "provider_name", command.getProviderName(),
                "city_code", command.getCityCode(),
                "status", command.getStatus()
            )
        );
        return serviceProviderConverter.toProviderResponse(buildAdminProviderEntity(requireProvider(providerId)));
    }

    @Transactional
    public ServiceProviderResponse createAdminServiceItem(
        Long providerId,
        AdminUpsertProviderServiceItemRequest request,
        AdminOperationContext operationContext
    ) {
        requireProvider(providerId);
        UpsertProviderServiceItemCommand command = buildServiceItemCommand(null, providerId, request);
        serviceCenterPersistenceMapper.insertServiceItem(command);
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_PROVIDER_SERVICE_ITEM,
            command.getServiceItemId().toString(),
            AUDIT_ACTION_PROVIDER_SERVICE_ITEM_CREATE,
            auditDetail(
                "provider_id", providerId,
                "service_code", command.getServiceCode(),
                "service_name", command.getServiceName(),
                "status", command.getStatus()
            )
        );
        return serviceProviderConverter.toProviderResponse(buildAdminProviderEntity(requireProvider(providerId)));
    }

    @Transactional
    public ServiceProviderResponse updateAdminServiceItem(
        Long providerId,
        Long serviceItemId,
        AdminUpsertProviderServiceItemRequest request,
        AdminOperationContext operationContext
    ) {
        requireProvider(providerId);
        if (serviceCenterPersistenceMapper.findServiceItemById(providerId, serviceItemId) == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "服务项目不存在");
        }
        UpsertProviderServiceItemCommand command = buildServiceItemCommand(serviceItemId, providerId, request);
        serviceCenterPersistenceMapper.updateServiceItem(command);
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_PROVIDER_SERVICE_ITEM,
            serviceItemId.toString(),
            AUDIT_ACTION_PROVIDER_SERVICE_ITEM_UPDATE,
            auditDetail(
                "provider_id", providerId,
                "service_code", command.getServiceCode(),
                "service_name", command.getServiceName(),
                "status", command.getStatus()
            )
        );
        return serviceProviderConverter.toProviderResponse(buildAdminProviderEntity(requireProvider(providerId)));
    }

    @Transactional
    public ServiceProviderResponse createAdminScheduleSlot(
        Long providerId,
        AdminUpsertProviderScheduleSlotRequest request,
        AdminOperationContext operationContext
    ) {
        requireProvider(providerId);
        UpsertProviderScheduleSlotCommand command = buildScheduleSlotCommand(null, providerId, request);
        serviceCenterPersistenceMapper.insertScheduleSlot(command);
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_PROVIDER_SCHEDULE_SLOT,
            command.getSlotId().toString(),
            AUDIT_ACTION_PROVIDER_SCHEDULE_SLOT_CREATE,
            auditDetail(
                "provider_id", providerId,
                "appointment_type", command.getAppointmentType(),
                "slot_date", command.getSlotDate(),
                "start_time", command.getStartTime(),
                "end_time", command.getEndTime(),
                "quota", command.getQuota(),
                "status", command.getStatus()
            )
        );
        return serviceProviderConverter.toProviderResponse(buildAdminProviderEntity(requireProvider(providerId)));
    }

    @Transactional
    public ServiceProviderResponse updateAdminScheduleSlot(
        Long providerId,
        Long slotId,
        AdminUpsertProviderScheduleSlotRequest request,
        AdminOperationContext operationContext
    ) {
        requireProvider(providerId);
        if (serviceCenterPersistenceMapper.findScheduleSlotById(providerId, slotId) == null) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND, "预约时段不存在");
        }
        UpsertProviderScheduleSlotCommand command = buildScheduleSlotCommand(slotId, providerId, request);
        serviceCenterPersistenceMapper.updateScheduleSlot(command);
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_PROVIDER_SCHEDULE_SLOT,
            slotId.toString(),
            AUDIT_ACTION_PROVIDER_SCHEDULE_SLOT_UPDATE,
            auditDetail(
                "provider_id", providerId,
                "appointment_type", command.getAppointmentType(),
                "slot_date", command.getSlotDate(),
                "start_time", command.getStartTime(),
                "end_time", command.getEndTime(),
                "quota", command.getQuota(),
                "status", command.getStatus()
            )
        );
        return serviceProviderConverter.toProviderResponse(buildAdminProviderEntity(requireProvider(providerId)));
    }

    public List<ServiceAppointmentResponse> listAdminAppointments(
        String status,
        String providerType,
        String cityCode
    ) {
        String normalizedStatus = normalizeNullableAppointmentStatus(status);
        String normalizedProviderType = normalizeNullableProviderType(providerType);
        String normalizedCityCode = normalizeNullableText(cityCode);
        return serviceCenterPersistenceMapper
            .listAdminAppointments(normalizedStatus, normalizedProviderType, normalizedCityCode)
            .stream()
            .map(serviceProviderConverter::toAppointmentEntity)
            .map(serviceProviderConverter::toAppointmentResponse)
            .toList();
    }

    public List<ProviderReviewResponse> listAdminReviews(
        String status,
        String providerType,
        String cityCode
    ) {
        String normalizedStatus = normalizeNullableReviewStatus(status);
        String normalizedProviderType = normalizeNullableProviderType(providerType);
        String normalizedCityCode = normalizeNullableText(cityCode);
        return serviceCenterPersistenceMapper
            .listAdminReviews(normalizedStatus, normalizedProviderType, normalizedCityCode)
            .stream()
            .map(serviceProviderConverter::toReviewEntity)
            .map(serviceProviderConverter::toReviewResponse)
            .toList();
    }

    @Transactional
    public ServiceAppointmentResponse updateAdminAppointmentStatus(
        Long appointmentId,
        AdminUpdateServiceAppointmentStatusRequest request,
        AdminOperationContext operationContext
    ) {
        ServiceAppointmentEntity appointment = requireAppointment(appointmentId);
        String normalizedStatus = normalizeAppointmentStatus(request.status());
        if ("canceled".equals(appointment.getStatus()) && !"canceled".equals(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "已取消预约不能恢复，请重新创建预约");
        }

        UpdateServiceAppointmentStatusCommand command = new UpdateServiceAppointmentStatusCommand();
        command.setAppointmentId(appointmentId);
        command.setStatus(normalizedStatus);
        command.setRemark(normalizeNullableText(request.remark()));
        int updatedRows = serviceCenterPersistenceMapper.updateAppointmentStatus(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.SERVICE_APPOINTMENT_NOT_FOUND);
        }

        if ("canceled".equals(normalizedStatus)
            && ("pending_confirm".equals(appointment.getStatus()) || "confirmed".equals(appointment.getStatus()))) {
            SlotWindow slotWindow = parseAppointmentSlot(appointment.getAppointmentSlot());
            serviceCenterPersistenceMapper.decreaseScheduleSlotBookedCount(
                appointment.getProviderId(),
                appointment.getAppointmentType(),
                appointment.getAppointmentDate(),
                slotWindow.startTime(),
                slotWindow.endTime()
            );
        }

        ServiceAppointmentEntity updatedAppointment = requireAppointment(appointmentId);
        timelineApplicationService.syncServiceAppointmentEvent(
            updatedAppointment.getPetId(),
            updatedAppointment.getAppointmentId(),
            updatedAppointment.getAppointmentDate().atTime(9, 0),
            buildTimelineTitle(updatedAppointment),
            buildTimelineSummary(updatedAppointment)
        );
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_SERVICE_APPOINTMENT,
            appointmentId.toString(),
            AUDIT_ACTION_SERVICE_APPOINTMENT_STATUS_UPDATE,
            auditDetail(
                "previous_status", appointment.getStatus(),
                "next_status", updatedAppointment.getStatus(),
                "provider_id", updatedAppointment.getProviderId(),
                "appointment_type", updatedAppointment.getAppointmentType(),
                "remark", command.getRemark()
            )
        );
        return serviceProviderConverter.toAppointmentResponse(updatedAppointment);
    }

    @Transactional
    public ProviderReviewResponse updateAdminReviewStatus(
        Long reviewId,
        AdminUpdateProviderReviewStatusRequest request,
        AdminOperationContext operationContext
    ) {
        ProviderReviewEntity review = requireReview(reviewId);
        UpdateProviderReviewStatusCommand command = new UpdateProviderReviewStatusCommand();
        command.setReviewId(reviewId);
        command.setStatus(normalizeReviewStatus(request.status()));
        int updatedRows = serviceCenterPersistenceMapper.updateReviewStatus(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.SERVICE_REVIEW_NOT_FOUND);
        }
        serviceCenterPersistenceMapper.refreshProviderReviewSummary(review.getProviderId());
        auditAdminOperation(
            operationContext,
            AUDIT_TARGET_PROVIDER_REVIEW,
            reviewId.toString(),
            AUDIT_ACTION_PROVIDER_REVIEW_STATUS_UPDATE,
            auditDetail(
                "previous_status", review.getStatus(),
                "next_status", command.getStatus(),
                "provider_id", review.getProviderId(),
                "rating", review.getRating()
            )
        );
        return serviceProviderConverter.toReviewResponse(requireReview(reviewId));
    }

    @Transactional
    public ServiceAppointmentResponse createAppointment(CreateServiceAppointmentRequest request) {
        Long currentUserId = CurrentUserContext.requireUserId();
        requireAccessiblePet(currentUserId, request.petId());
        ServiceProviderDataObject provider = requireProvider(request.providerId());
        requireOpenedCity(provider.cityCode());
        if (!"online".equals(provider.status())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "服务商当前不可预约");
        }
        String normalizedAppointmentType = normalizeAppointmentType(request.appointmentType());
        if (!provider.providerType().equals(normalizedAppointmentType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "预约类型与服务商类型不匹配");
        }
        LocalDate appointmentDate = normalizeAppointmentDate(request.appointmentDate());
        SlotWindow slotWindow = parseAppointmentSlot(request.appointmentSlot());
        ProviderScheduleSlotDataObject scheduleSlot = serviceCenterPersistenceMapper.findScheduleSlotForUpdate(
            provider.providerId(),
            normalizedAppointmentType,
            appointmentDate,
            slotWindow.startTime(),
            slotWindow.endTime()
        );
        validateScheduleSlot(scheduleSlot);
        int increasedRows = serviceCenterPersistenceMapper.increaseScheduleSlotBookedCount(scheduleSlot.slotId());
        if (increasedRows == 0) {
            throw new BusinessException(ResponseCode.APPOINTMENT_SLOT_INVALID);
        }

        CreateServiceAppointmentCommand command = new CreateServiceAppointmentCommand();
        command.setUserId(currentUserId);
        command.setPetId(request.petId());
        command.setProviderId(provider.providerId());
        command.setAppointmentType(normalizedAppointmentType);
        command.setAppointmentDate(appointmentDate);
        command.setAppointmentSlot(formatAppointmentSlot(slotWindow));
        command.setDemandDesc(normalizeNullableText(request.demandDesc()));
        command.setContactName(normalizeRequiredText(request.contactName(), "联系人不能为空"));
        command.setContactMobile(normalizeRequiredText(request.contactMobile(), "联系电话不能为空"));
        serviceCenterPersistenceMapper.insertAppointment(command);

        ServiceAppointmentEntity appointment = requireAppointment(command.getId());
        syncAppointmentDerivedModels(currentUserId, appointment);
        return serviceProviderConverter.toAppointmentResponse(appointment);
    }

    @Transactional
    public ServiceAppointmentResponse cancelAppointment(
        Long appointmentId,
        CancelServiceAppointmentRequest request
    ) {
        Long currentUserId = CurrentUserContext.requireUserId();
        ServiceAppointmentEntity appointment = requireUserAppointment(currentUserId, appointmentId);
        SlotWindow slotWindow = parseAppointmentSlot(appointment.getAppointmentSlot());

        CancelServiceAppointmentCommand command = new CancelServiceAppointmentCommand();
        command.setAppointmentId(appointmentId);
        command.setUserId(currentUserId);
        command.setCancelReason(normalizeNullableText(request == null ? null : request.cancelReason()));
        int updatedRows = serviceCenterPersistenceMapper.cancelAppointment(command);
        if (updatedRows == 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "当前预约状态不允许取消");
        }
        serviceCenterPersistenceMapper.decreaseScheduleSlotBookedCount(
            appointment.getProviderId(),
            appointment.getAppointmentType(),
            appointment.getAppointmentDate(),
            slotWindow.startTime(),
            slotWindow.endTime()
        );

        ServiceAppointmentEntity canceledAppointment = requireAppointment(appointmentId);
        syncAppointmentDerivedModels(currentUserId, canceledAppointment);
        return serviceProviderConverter.toAppointmentResponse(canceledAppointment);
    }

    @Transactional
    public ProviderReviewResponse createProviderReview(
        Long appointmentId,
        CreateProviderReviewRequest request
    ) {
        Long currentUserId = CurrentUserContext.requireUserId();
        ServiceAppointmentEntity appointment = requireUserAppointment(currentUserId, appointmentId);
        if (!"completed".equals(appointment.getStatus())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "只有已完成的服务预约可以评价");
        }
        if (appointment.isReviewed()
            || serviceCenterPersistenceMapper.findReviewByAppointmentId(appointmentId) != null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "该预约已经评价过");
        }

        CreateProviderReviewCommand command = new CreateProviderReviewCommand();
        command.setProviderId(appointment.getProviderId());
        command.setAppointmentId(appointment.getAppointmentId());
        command.setUserId(currentUserId);
        command.setPetId(appointment.getPetId());
        command.setRating(request.rating());
        command.setContent(normalizeNullableText(request.content()));
        serviceCenterPersistenceMapper.insertReview(command);
        serviceCenterPersistenceMapper.refreshProviderReviewSummary(appointment.getProviderId());
        return serviceProviderConverter.toReviewResponse(requireReview(command.getReviewId()));
    }

    private List<ServiceProviderResponse> toProviderResponses(List<ServiceProviderDataObject> providers) {
        return providers.stream()
            .map(provider -> buildProviderEntity(provider, null, 7))
            .map(serviceProviderConverter::toProviderResponse)
            .toList();
    }

    private ServiceProviderEntity buildProviderEntity(
        ServiceProviderDataObject provider,
        String appointmentType,
        int days
    ) {
        List<ProviderServiceItemEntity> serviceItems = serviceCenterPersistenceMapper
            .listServiceItemsByProviderId(provider.providerId())
            .stream()
            .map(serviceProviderConverter::toServiceItemEntity)
            .toList();
        List<ProviderScheduleSlotEntity> availableSlots = serviceCenterPersistenceMapper
            .listScheduleSlots(
                provider.providerId(),
                appointmentType,
                LocalDate.now(),
                LocalDate.now().plusDays(days)
            )
            .stream()
            .map(serviceProviderConverter::toScheduleSlotEntity)
            .filter(ProviderScheduleSlotEntity::isBookable)
            .limit(6)
            .toList();
        return serviceProviderConverter.toProviderEntity(provider, serviceItems, availableSlots);
    }

    private void syncAppointmentDerivedModels(Long actorUserId, ServiceAppointmentEntity appointment) {
        timelineApplicationService.syncServiceAppointmentEvent(
            appointment.getPetId(),
            appointment.getAppointmentId(),
            appointment.getAppointmentDate().atTime(9, 0),
            buildTimelineTitle(appointment),
            buildTimelineSummary(appointment)
        );
        if ("pending_confirm".equals(appointment.getStatus())) {
            notificationApplicationService.createAppointmentCreatedNotification(actorUserId, appointment);
        }
    }

    private String buildTimelineTitle(ServiceAppointmentEntity appointment) {
        if ("canceled".equals(appointment.getStatus())) {
            return "已取消" + providerTypeTitle(appointment.getAppointmentType()) + "预约";
        }
        return providerTypeTitle(appointment.getAppointmentType()) + "预约";
    }

    private String buildTimelineSummary(ServiceAppointmentEntity appointment) {
        return "%s · %s · %s".formatted(
            appointment.getProviderName(),
            appointment.getAppointmentDate(),
            appointment.getAppointmentSlot()
        );
    }

    private ServiceProviderEntity buildAdminProviderEntity(ServiceProviderDataObject provider) {
        List<ProviderServiceItemEntity> serviceItems = serviceCenterPersistenceMapper
            .listAllServiceItemsByProviderId(provider.providerId())
            .stream()
            .map(serviceProviderConverter::toServiceItemEntity)
            .toList();
        List<ProviderScheduleSlotEntity> availableSlots = serviceCenterPersistenceMapper
            .listScheduleSlots(
                provider.providerId(),
                null,
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(30)
            )
            .stream()
            .map(serviceProviderConverter::toScheduleSlotEntity)
            .toList();
        return serviceProviderConverter.toProviderEntity(provider, serviceItems, availableSlots);
    }

    private void auditAdminOperation(
        AdminOperationContext operationContext,
        String targetType,
        String targetId,
        String action,
        Map<String, Object> detail
    ) {
        auditLogApplicationService.recordAdminOperation(operationContext, targetType, targetId, action, detail);
    }

    /**
     * 审计详情需要保留结构化字段，后台查询时可直接看到关键状态变化。
     */
    private Map<String, Object> auditDetail(Object... keyValues) {
        Map<String, Object> detail = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            detail.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return detail;
    }

    private UpsertServiceProviderCommand buildProviderCommand(
        Long providerId,
        AdminUpsertServiceProviderRequest request
    ) {
        UpsertServiceProviderCommand command = new UpsertServiceProviderCommand();
        command.setProviderId(providerId);
        command.setProviderType(normalizeAppointmentType(request.providerType()));
        command.setProviderName(normalizeRequiredText(request.providerName(), "服务商名称不能为空"));
        command.setCityCode(normalizeRequiredText(request.cityCode(), "城市编码不能为空"));
        command.setAddress(normalizeNullableText(request.address()));
        command.setLatitude(request.latitude());
        command.setLongitude(request.longitude());
        command.setContactPhone(normalizeNullableText(request.contactPhone()));
        command.setBusinessHours(normalizeNullableText(request.businessHours()));
        command.setRatingAvg(normalizeRating(request.ratingAvg()));
        command.setReviewCount(request.reviewCount() == null ? 0 : Math.max(0, request.reviewCount()));
        command.setStatus(normalizeProviderStatus(request.status()));
        return command;
    }

    private UpsertServiceCityConfigCommand buildCityConfigCommand(AdminUpsertServiceCityConfigRequest request) {
        boolean opened = Boolean.TRUE.equals(request.opened());
        UpsertServiceCityConfigCommand command = new UpsertServiceCityConfigCommand();
        command.setCityCode(normalizeRequiredText(request.cityCode(), "城市编码不能为空"));
        command.setCityName(normalizeRequiredText(request.cityName(), "城市名称不能为空"));
        command.setOpened(opened);
        command.setUnavailableReason(opened ? null : resolveConfiguredUnavailableReason(request.unavailableReason()));
        command.setSortOrder(request.sortOrder() == null ? 0 : Math.max(0, request.sortOrder()));
        return command;
    }

    private UpsertProviderServiceItemCommand buildServiceItemCommand(
        Long serviceItemId,
        Long providerId,
        AdminUpsertProviderServiceItemRequest request
    ) {
        validatePriceRange(request.priceMin(), request.priceMax());
        UpsertProviderServiceItemCommand command = new UpsertProviderServiceItemCommand();
        command.setServiceItemId(serviceItemId);
        command.setProviderId(providerId);
        command.setServiceCode(normalizeRequiredText(request.serviceCode(), "服务编码不能为空"));
        command.setServiceName(normalizeRequiredText(request.serviceName(), "服务名称不能为空"));
        command.setServiceDesc(normalizeNullableText(request.serviceDesc()));
        command.setPriceMin(request.priceMin());
        command.setPriceMax(request.priceMax());
        command.setStatus(normalizeServiceItemStatus(request.status()));
        return command;
    }

    private UpsertProviderScheduleSlotCommand buildScheduleSlotCommand(
        Long slotId,
        Long providerId,
        AdminUpsertProviderScheduleSlotRequest request
    ) {
        if (request.endTime() == null || request.startTime() == null || !request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        UpsertProviderScheduleSlotCommand command = new UpsertProviderScheduleSlotCommand();
        command.setSlotId(slotId);
        command.setProviderId(providerId);
        command.setAppointmentType(normalizeAppointmentType(request.appointmentType()));
        command.setSlotDate(request.slotDate());
        command.setStartTime(request.startTime());
        command.setEndTime(request.endTime());
        command.setQuota(request.quota());
        command.setStatus(normalizeSlotStatus(request.status()));
        return command;
    }

    private ServiceCategoryResponse buildCategoryResponse(String providerType, int providerCount, boolean cityOpened) {
        ServiceCategoryDescriptor descriptor = CATEGORY_DESCRIPTORS.get(providerType);
        return new ServiceCategoryResponse(
            providerType,
            descriptor.title(),
            descriptor.description(),
            providerCount,
            cityOpened && providerCount > 0
        );
    }

    private ServiceProviderDataObject requireProvider(Long providerId) {
        ServiceProviderDataObject provider = serviceCenterPersistenceMapper.findProviderById(providerId);
        if (provider == null) {
            throw new BusinessException(ResponseCode.SERVICE_PROVIDER_NOT_FOUND);
        }
        return provider;
    }

    private ServiceAppointmentEntity requireAppointment(Long appointmentId) {
        ServiceAppointmentEntity appointment = serviceProviderConverter.toAppointmentEntity(
            serviceCenterPersistenceMapper.findAppointmentById(appointmentId)
        );
        if (appointment == null) {
            throw new BusinessException(ResponseCode.SERVICE_APPOINTMENT_NOT_FOUND);
        }
        return appointment;
    }

    private ProviderReviewEntity requireReview(Long reviewId) {
        ProviderReviewEntity review = serviceProviderConverter.toReviewEntity(
            serviceCenterPersistenceMapper.findReviewById(reviewId)
        );
        if (review == null) {
            throw new BusinessException(ResponseCode.SERVICE_REVIEW_NOT_FOUND);
        }
        return review;
    }

    private ServiceAppointmentEntity requireUserAppointment(Long currentUserId, Long appointmentId) {
        ServiceAppointmentEntity appointment = serviceProviderConverter.toAppointmentEntity(
            serviceCenterPersistenceMapper.findAppointmentByUserIdAndId(currentUserId, appointmentId)
        );
        if (appointment == null) {
            throw new BusinessException(ResponseCode.SERVICE_APPOINTMENT_NOT_FOUND);
        }
        return appointment;
    }

    private void requireAccessiblePet(Long userId, Long petId) {
        if (petPersistenceMapper.findAccessiblePetById(userId, petId) == null) {
            throw new BusinessException(ResponseCode.PET_NOT_FOUND);
        }
    }

    private void requireOpenedCity(String cityCode) {
        ServiceCityConfigEntity cityConfig = resolveCityConfig(
            new CityContext(cityCode, cityCode == null ? DEFAULT_CITY_NAME : cityCode)
        );
        if (!cityConfig.isOpened()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, resolveUnavailableReason(cityConfig));
        }
    }

    private ServiceCityConfigEntity resolveCityConfig(CityContext cityContext) {
        ServiceCityConfigEntity cityConfig = serviceProviderConverter.toCityConfigEntity(
            serviceCenterPersistenceMapper.findCityConfigByCityCode(cityContext.cityCode())
        );
        if (cityConfig != null) {
            return cityConfig;
        }
        return new ServiceCityConfigEntity(
            null,
            cityContext.cityCode(),
            cityContext.cityName(),
            false,
            DEFAULT_UNAVAILABLE_REASON,
            0,
            null,
            null
        );
    }

    private CityContext resolveCityContext(Long currentUserId, String cityCode) {
        UserSettingsDataObject userSettings = userPersistenceMapper.findUserSettingsByUserId(currentUserId);
        String requestedCityCode = normalizeNullableText(cityCode);
        String userCityCode = normalizeNullableText(userSettings == null ? null : userSettings.cityCode());
        String normalizedCityCode = requestedCityCode == null ? userCityCode : requestedCityCode;
        String cityName = null;
        if (requestedCityCode == null || requestedCityCode.equals(userCityCode)) {
            cityName = normalizeNullableText(userSettings == null ? null : userSettings.cityName());
        }
        return new CityContext(
            normalizedCityCode == null ? DEFAULT_CITY_CODE : normalizedCityCode,
            cityName == null ? (normalizedCityCode == null ? DEFAULT_CITY_NAME : normalizedCityCode) : cityName
        );
    }

    private String resolveUnavailableReason(ServiceCityConfigEntity cityConfig) {
        return resolveConfiguredUnavailableReason(cityConfig.getUnavailableReason());
    }

    private String resolveConfiguredUnavailableReason(String unavailableReason) {
        String normalizedReason = normalizeNullableText(unavailableReason);
        return normalizedReason == null ? DEFAULT_UNAVAILABLE_REASON : normalizedReason;
    }

    private String normalizeNullableProviderType(String providerType) {
        String normalizedProviderType = normalizeNullableText(providerType);
        if (normalizedProviderType == null) {
            return null;
        }
        return normalizeAppointmentType(normalizedProviderType);
    }

    private String normalizeNullableProviderStatus(String status) {
        String normalizedStatus = normalizeNullableText(status);
        if (normalizedStatus == null || "all".equals(normalizedStatus)) {
            return null;
        }
        return normalizeProviderStatus(normalizedStatus);
    }

    private String normalizeAppointmentType(String appointmentType) {
        String normalizedAppointmentType = normalizeRequiredText(appointmentType, "服务类型不能为空");
        if (!SUPPORTED_PROVIDER_TYPES.contains(normalizedAppointmentType)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "服务类型仅支持医院、寄养、洗护或训练");
        }
        return normalizedAppointmentType;
    }

    private String normalizeNullableAppointmentStatus(String status) {
        String normalizedStatus = normalizeNullableText(status);
        if (normalizedStatus == null || "all".equals(normalizedStatus)) {
            return null;
        }
        if (!SUPPORTED_APPOINTMENT_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "预约状态不支持");
        }
        return normalizedStatus;
    }

    private String normalizeAppointmentStatus(String status) {
        String normalizedStatus = normalizeRequiredText(status, "预约状态不能为空");
        if (!SUPPORTED_APPOINTMENT_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "预约状态不支持");
        }
        return normalizedStatus;
    }

    private String normalizeProviderStatus(String status) {
        String normalizedStatus = normalizeRequiredText(status, "服务商状态不能为空");
        if (!SUPPORTED_PROVIDER_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "服务商状态仅支持 online、rest 或 offline");
        }
        return normalizedStatus;
    }

    private String normalizeServiceItemStatus(String status) {
        String normalizedStatus = normalizeRequiredText(status, "服务项目状态不能为空");
        if (!SUPPORTED_SERVICE_ITEM_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "服务项目状态仅支持 active 或 inactive");
        }
        return normalizedStatus;
    }

    private String normalizeSlotStatus(String status) {
        String normalizedStatus = normalizeRequiredText(status, "时段状态不能为空");
        if (!SUPPORTED_SLOT_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "时段状态仅支持 open、closed 或 full");
        }
        return normalizedStatus;
    }

    private String normalizeNullableReviewStatus(String status) {
        String normalizedStatus = normalizeNullableText(status);
        if (normalizedStatus == null || "all".equals(normalizedStatus)) {
            return null;
        }
        return normalizeReviewStatus(normalizedStatus);
    }

    private String normalizeReviewStatus(String status) {
        String normalizedStatus = normalizeRequiredText(status, "评价状态不能为空");
        if (!SUPPORTED_REVIEW_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "评价状态仅支持 visible 或 hidden");
        }
        return normalizedStatus;
    }

    private BigDecimal normalizeRating(BigDecimal rating) {
        if (rating == null) {
            return null;
        }
        if (rating.compareTo(BigDecimal.ZERO) < 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "评分必须在 0 到 5 之间");
        }
        return rating;
    }

    private void validatePriceRange(BigDecimal priceMin, BigDecimal priceMax) {
        if (priceMin != null && priceMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "最低价格不能小于 0");
        }
        if (priceMax != null && priceMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "最高价格不能小于 0");
        }
        if (priceMin != null && priceMax != null && priceMax.compareTo(priceMin) < 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "最高价格不能低于最低价格");
        }
    }

    private LocalDate normalizeAppointmentDate(LocalDate appointmentDate) {
        if (appointmentDate == null || appointmentDate.isBefore(LocalDate.now())) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, "预约日期不能早于今天");
        }
        return appointmentDate;
    }

    /**
     * 预约时段必须由服务端按固定格式解析并和库存时段表匹配，不能信任前端自由文本。
     */
    private SlotWindow parseAppointmentSlot(String appointmentSlot) {
        String normalizedAppointmentSlot = normalizeRequiredText(appointmentSlot, "预约时段不能为空");
        String[] parts = normalizedAppointmentSlot.split("-");
        if (parts.length != 2) {
            throw new BusinessException(ResponseCode.APPOINTMENT_SLOT_INVALID);
        }
        try {
            LocalTime startTime = LocalTime.parse(parts[0].trim(), SLOT_TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(parts[1].trim(), SLOT_TIME_FORMATTER);
            if (!endTime.isAfter(startTime)) {
                throw new BusinessException(ResponseCode.APPOINTMENT_SLOT_INVALID);
            }
            return new SlotWindow(startTime, endTime);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ResponseCode.APPOINTMENT_SLOT_INVALID);
        }
    }

    private void validateScheduleSlot(ProviderScheduleSlotDataObject scheduleSlot) {
        if (scheduleSlot == null || !"open".equals(scheduleSlot.status())) {
            throw new BusinessException(ResponseCode.APPOINTMENT_SLOT_INVALID);
        }
        ProviderScheduleSlotEntity slotEntity = serviceProviderConverter.toScheduleSlotEntity(scheduleSlot);
        if (!slotEntity.isBookable()) {
            throw new BusinessException(ResponseCode.APPOINTMENT_SLOT_INVALID);
        }
    }

    private String formatAppointmentSlot(SlotWindow slotWindow) {
        return slotWindow.startTime().format(SLOT_TIME_FORMATTER) + "-" + slotWindow.endTime().format(SLOT_TIME_FORMATTER);
    }

    private String providerTypeTitle(String appointmentType) {
        ServiceCategoryDescriptor descriptor = CATEGORY_DESCRIPTORS.get(appointmentType);
        return descriptor == null ? "服务" : descriptor.title();
    }

    private String normalizeRequiredText(String text, String message) {
        String normalizedText = normalizeNullableText(text);
        if (normalizedText == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, message);
        }
        return normalizedText;
    }

    private String normalizeNullableText(String text) {
        if (text == null) {
            return null;
        }
        String normalizedText = text.trim();
        return normalizedText.isEmpty() ? null : normalizedText;
    }

    private record ServiceCategoryDescriptor(String title, String description) {
    }

    private record CityContext(String cityCode, String cityName) {
    }

    private record SlotWindow(LocalTime startTime, LocalTime endTime) {
    }
}
