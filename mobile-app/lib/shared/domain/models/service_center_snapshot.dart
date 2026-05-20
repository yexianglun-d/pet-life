/// 服务中心首页快照。
class ServiceHomeSnapshot {
  const ServiceHomeSnapshot({
    required this.cityCode,
    required this.cityName,
    required this.opened,
    required this.categories,
    required this.featuredProviders,
    required this.upcomingAppointments,
    required this.commercePlaceholder,
    this.unavailableReason,
  });

  final String cityCode;
  final String cityName;
  final bool opened;
  final String? unavailableReason;
  final List<ServiceCategorySnapshot> categories;
  final List<ServiceProviderSnapshot> featuredProviders;
  final List<ServiceAppointmentSnapshot> upcomingAppointments;
  final String commercePlaceholder;
}

/// 服务分类快照。
class ServiceCategorySnapshot {
  const ServiceCategorySnapshot({
    required this.providerType,
    required this.title,
    required this.description,
    required this.providerCount,
    required this.available,
  });

  final String providerType;
  final String title;
  final String description;
  final int providerCount;
  final bool available;
}

/// 服务商快照。
class ServiceProviderSnapshot {
  const ServiceProviderSnapshot({
    required this.providerId,
    required this.providerType,
    required this.providerName,
    required this.cityCode,
    required this.status,
    required this.bookable,
    required this.serviceItems,
    required this.availableSlots,
    this.address,
    this.contactPhone,
    this.businessHours,
    this.latitude,
    this.longitude,
    this.coordinateSource,
    this.distanceMeters,
    this.ratingAvg,
    this.reviewCount = 0,
  });

  final String providerId;
  final String providerType;
  final String providerName;
  final String cityCode;
  final String? address;
  final String? contactPhone;
  final String? businessHours;
  final double? latitude;
  final double? longitude;
  final String? coordinateSource;
  final int? distanceMeters;
  final String? ratingAvg;
  final int reviewCount;
  final String status;
  final bool bookable;
  final List<ProviderServiceItemSnapshot> serviceItems;
  final List<ProviderScheduleSlotSnapshot> availableSlots;

  bool get hasCoordinate => latitude != null && longitude != null;
}

/// 服务项目快照。
class ProviderServiceItemSnapshot {
  const ProviderServiceItemSnapshot({
    required this.serviceItemId,
    required this.serviceCode,
    required this.serviceName,
    required this.status,
    this.serviceDesc,
    this.priceMin,
    this.priceMax,
  });

  final String serviceItemId;
  final String serviceCode;
  final String serviceName;
  final String? serviceDesc;
  final String? priceMin;
  final String? priceMax;
  final String status;
}

/// 可预约时段快照。
class ProviderScheduleSlotSnapshot {
  const ProviderScheduleSlotSnapshot({
    required this.slotId,
    required this.providerId,
    required this.appointmentType,
    required this.slotDate,
    required this.startTime,
    required this.endTime,
    required this.quota,
    required this.bookedCount,
    required this.availableQuota,
    required this.status,
    required this.bookable,
  });

  final String slotId;
  final String providerId;
  final String appointmentType;
  final DateTime slotDate;
  final String startTime;
  final String endTime;
  final int quota;
  final int bookedCount;
  final int availableQuota;
  final String status;
  final bool bookable;

  String get displayText => '$startTime-$endTime';
}

/// 服务预约草稿。
class ServiceAppointmentDraft {
  const ServiceAppointmentDraft({
    required this.petId,
    required this.providerId,
    required this.appointmentType,
    required this.appointmentDate,
    required this.appointmentSlot,
    required this.contactName,
    required this.contactMobile,
    this.demandDesc,
  });

  final String petId;
  final String providerId;
  final String appointmentType;
  final DateTime appointmentDate;
  final String appointmentSlot;
  final String? demandDesc;
  final String contactName;
  final String contactMobile;
}

/// 服务评价草稿。
class ServiceReviewDraft {
  const ServiceReviewDraft({
    required this.rating,
    this.content,
  });

  final int rating;
  final String? content;
}

/// 服务预约快照。
class ServiceAppointmentSnapshot {
  const ServiceAppointmentSnapshot({
    required this.appointmentId,
    required this.petId,
    required this.petName,
    required this.providerId,
    required this.providerName,
    required this.providerType,
    required this.appointmentType,
    required this.appointmentDate,
    required this.appointmentSlot,
    required this.contactName,
    required this.contactMobile,
    required this.status,
    required this.reviewed,
    this.demandDesc,
    this.remark,
    this.createdAt,
    this.updatedAt,
  });

  final String appointmentId;
  final String petId;
  final String petName;
  final String providerId;
  final String providerName;
  final String providerType;
  final String appointmentType;
  final DateTime appointmentDate;
  final String appointmentSlot;
  final String? demandDesc;
  final String contactName;
  final String contactMobile;
  final String status;
  final bool reviewed;
  final String? remark;
  final DateTime? createdAt;
  final DateTime? updatedAt;
}

/// 服务商评价快照。
class ProviderReviewSnapshot {
  const ProviderReviewSnapshot({
    required this.reviewId,
    required this.providerId,
    required this.providerName,
    required this.providerType,
    required this.userId,
    required this.reviewerNickname,
    required this.rating,
    required this.status,
    required this.createdAt,
    required this.updatedAt,
    this.appointmentId,
    this.petId,
    this.petName,
    this.content,
  });

  final String reviewId;
  final String providerId;
  final String providerName;
  final String providerType;
  final String? appointmentId;
  final String userId;
  final String reviewerNickname;
  final String? petId;
  final String? petName;
  final int rating;
  final String? content;
  final String status;
  final DateTime createdAt;
  final DateTime updatedAt;
}
