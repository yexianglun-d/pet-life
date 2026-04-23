/// 宠物详情快照。
class PetDetailSnapshot {
  const PetDetailSnapshot({
    required this.petId,
    required this.petName,
    required this.petType,
    required this.breed,
    required this.gender,
    required this.neuterStatus,
    this.birthday,
    this.adoptDate,
    this.avatarUrl,
    this.weightKg,
    this.allergyNotes,
    this.medicalHistory,
    this.status = 'active',
    this.createdAt,
    this.updatedAt,
  });

  final String petId;
  final String petName;
  final String petType;
  final String breed;
  final String gender;
  final String neuterStatus;
  final DateTime? birthday;
  final DateTime? adoptDate;
  final String? avatarUrl;
  final String? weightKg;
  final String? allergyNotes;
  final String? medicalHistory;
  final String status;
  final DateTime? createdAt;
  final DateTime? updatedAt;
}

/// 宠物表单草稿。
///
/// 创建与编辑宠物共用同一份字段模型，避免页面层分别维护两套请求结构。
class PetUpsertDraft {
  const PetUpsertDraft({
    required this.petName,
    required this.petType,
    required this.breed,
    required this.gender,
    required this.neuterStatus,
    this.birthday,
    this.adoptDate,
    this.avatarAssetId,
    this.weightKg,
    this.allergyNotes,
    this.medicalHistory,
  });

  final String petName;
  final String petType;
  final String breed;
  final String gender;
  final String neuterStatus;
  final DateTime? birthday;
  final DateTime? adoptDate;
  final String? avatarAssetId;
  final String? weightKg;
  final String? allergyNotes;
  final String? medicalHistory;
}
