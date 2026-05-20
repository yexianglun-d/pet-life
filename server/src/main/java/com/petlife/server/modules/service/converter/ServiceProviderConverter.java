package com.petlife.server.modules.service.converter;

import com.petlife.server.common.time.DateTimeConverters;
import com.petlife.server.modules.service.domain.entity.ProviderReviewEntity;
import com.petlife.server.modules.service.domain.entity.ProviderScheduleSlotEntity;
import com.petlife.server.modules.service.domain.entity.ProviderServiceItemEntity;
import com.petlife.server.modules.service.domain.entity.ServiceAppointmentEntity;
import com.petlife.server.modules.service.domain.entity.ServiceCityConfigEntity;
import com.petlife.server.modules.service.domain.entity.ServiceProviderEntity;
import com.petlife.server.modules.service.dto.response.ProviderReviewResponse;
import com.petlife.server.modules.service.dto.response.ProviderScheduleSlotResponse;
import com.petlife.server.modules.service.dto.response.ProviderServiceItemResponse;
import com.petlife.server.modules.service.dto.response.ServiceAppointmentResponse;
import com.petlife.server.modules.service.dto.response.ServiceCityConfigResponse;
import com.petlife.server.modules.service.dto.response.ServiceProviderResponse;
import com.petlife.server.modules.service.persistence.dataobject.ProviderReviewDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ProviderScheduleSlotDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ProviderServiceItemDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceAppointmentDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceCityConfigDataObject;
import com.petlife.server.modules.service.persistence.dataobject.ServiceProviderDataObject;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 服务中心领域转换器。
 */
@Component
public class ServiceProviderConverter {

    public ServiceProviderEntity toProviderEntity(
        ServiceProviderDataObject dataObject,
        List<ProviderServiceItemEntity> serviceItems,
        List<ProviderScheduleSlotEntity> availableSlots
    ) {
        return toProviderEntity(dataObject, serviceItems, availableSlots, null);
    }

    public ServiceProviderEntity toProviderEntity(
        ServiceProviderDataObject dataObject,
        List<ProviderServiceItemEntity> serviceItems,
        List<ProviderScheduleSlotEntity> availableSlots,
        Integer distanceMeters
    ) {
        if (dataObject == null) {
            return null;
        }
        return new ServiceProviderEntity(
            dataObject.providerId(),
            dataObject.providerType(),
            dataObject.providerName(),
            dataObject.cityCode(),
            dataObject.address(),
            dataObject.latitude(),
            dataObject.longitude(),
            dataObject.coordinateSource(),
            distanceMeters,
            dataObject.contactPhone(),
            dataObject.businessHours(),
            dataObject.ratingAvg(),
            dataObject.reviewCount(),
            dataObject.status(),
            serviceItems,
            availableSlots,
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public ServiceCityConfigEntity toCityConfigEntity(ServiceCityConfigDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new ServiceCityConfigEntity(
            dataObject.configId(),
            dataObject.cityCode(),
            dataObject.cityName(),
            Boolean.TRUE.equals(dataObject.opened()),
            dataObject.unavailableReason(),
            dataObject.sortOrder(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public ProviderServiceItemEntity toServiceItemEntity(ProviderServiceItemDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new ProviderServiceItemEntity(
            dataObject.serviceItemId(),
            dataObject.providerId(),
            dataObject.serviceCode(),
            dataObject.serviceName(),
            dataObject.serviceDesc(),
            dataObject.priceMin(),
            dataObject.priceMax(),
            dataObject.status()
        );
    }

    public ProviderScheduleSlotEntity toScheduleSlotEntity(ProviderScheduleSlotDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new ProviderScheduleSlotEntity(
            dataObject.slotId(),
            dataObject.providerId(),
            dataObject.appointmentType(),
            dataObject.slotDate(),
            dataObject.startTime(),
            dataObject.endTime(),
            dataObject.quota(),
            dataObject.bookedCount(),
            dataObject.status()
        );
    }

    public ServiceAppointmentEntity toAppointmentEntity(ServiceAppointmentDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new ServiceAppointmentEntity(
            dataObject.appointmentId(),
            dataObject.userId(),
            dataObject.petId(),
            dataObject.petName(),
            dataObject.providerId(),
            dataObject.providerName(),
            dataObject.providerType(),
            dataObject.appointmentType(),
            dataObject.appointmentDate(),
            dataObject.appointmentSlot(),
            dataObject.demandDesc(),
            dataObject.contactName(),
            dataObject.contactMobile(),
            dataObject.status(),
            dataObject.remark(),
            Boolean.TRUE.equals(dataObject.reviewed()),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public ProviderReviewEntity toReviewEntity(ProviderReviewDataObject dataObject) {
        if (dataObject == null) {
            return null;
        }
        return new ProviderReviewEntity(
            dataObject.reviewId(),
            dataObject.providerId(),
            dataObject.providerName(),
            dataObject.providerType(),
            dataObject.appointmentId(),
            dataObject.userId(),
            dataObject.reviewerNickname(),
            dataObject.petId(),
            dataObject.petName(),
            dataObject.rating(),
            dataObject.content(),
            dataObject.status(),
            dataObject.createdAt(),
            dataObject.updatedAt()
        );
    }

    public ServiceProviderResponse toProviderResponse(ServiceProviderEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ServiceProviderResponse(
            entity.getProviderId().toString(),
            entity.getProviderType(),
            entity.getProviderName(),
            entity.getCityCode(),
            entity.getAddress(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.getCoordinateSource(),
            entity.getDistanceMeters(),
            entity.getContactPhone(),
            entity.getBusinessHours(),
            entity.getRatingAvg(),
            entity.getReviewCount(),
            entity.getStatus(),
            entity.isBookable(),
            entity.getServiceItems().stream().map(this::toServiceItemResponse).toList(),
            entity.getAvailableSlots().stream().map(this::toScheduleSlotResponse).toList(),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    public ServiceCityConfigResponse toCityConfigResponse(ServiceCityConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ServiceCityConfigResponse(
            entity.getConfigId() == null ? null : entity.getConfigId().toString(),
            entity.getCityCode(),
            entity.getCityName(),
            entity.isOpened(),
            entity.getUnavailableReason(),
            entity.getSortOrder(),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    public ProviderServiceItemResponse toServiceItemResponse(ProviderServiceItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProviderServiceItemResponse(
            entity.getServiceItemId().toString(),
            entity.getServiceCode(),
            entity.getServiceName(),
            entity.getServiceDesc(),
            entity.getPriceMin(),
            entity.getPriceMax(),
            entity.getStatus()
        );
    }

    public ProviderScheduleSlotResponse toScheduleSlotResponse(ProviderScheduleSlotEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProviderScheduleSlotResponse(
            entity.getSlotId().toString(),
            entity.getProviderId().toString(),
            entity.getAppointmentType(),
            entity.getSlotDate(),
            entity.getStartTime(),
            entity.getEndTime(),
            entity.getQuota(),
            entity.getBookedCount(),
            entity.getAvailableQuota(),
            entity.getStatus(),
            entity.isBookable()
        );
    }

    public ServiceAppointmentResponse toAppointmentResponse(ServiceAppointmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ServiceAppointmentResponse(
            entity.getAppointmentId().toString(),
            entity.getPetId().toString(),
            entity.getPetName(),
            entity.getProviderId().toString(),
            entity.getProviderName(),
            entity.getProviderType(),
            entity.getAppointmentType(),
            entity.getAppointmentDate(),
            entity.getAppointmentSlot(),
            entity.getDemandDesc(),
            entity.getContactName(),
            entity.getContactMobile(),
            entity.getStatus(),
            entity.getRemark(),
            entity.isReviewed(),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUpdatedAt())
        );
    }

    public ProviderReviewResponse toReviewResponse(ProviderReviewEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ProviderReviewResponse(
            entity.getReviewId().toString(),
            entity.getProviderId().toString(),
            entity.getProviderName(),
            entity.getProviderType(),
            entity.getAppointmentId() == null ? null : entity.getAppointmentId().toString(),
            entity.getUserId().toString(),
            entity.getReviewerNickname(),
            entity.getPetId() == null ? null : entity.getPetId().toString(),
            entity.getPetName(),
            entity.getRating(),
            entity.getContent(),
            entity.getStatus(),
            DateTimeConverters.toOffsetDateTime(entity.getCreatedAt()),
            DateTimeConverters.toOffsetDateTime(entity.getUpdatedAt())
        );
    }
}
