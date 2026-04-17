/// 宠物资料快照。
class PetProfileSnapshot {
  const PetProfileSnapshot({
    required this.petId,
    required this.petName,
    required this.petType,
    required this.breed,
    this.gender,
  });

  final String petId;
  final String petName;
  final String petType;
  final String breed;
  final String? gender;
}
