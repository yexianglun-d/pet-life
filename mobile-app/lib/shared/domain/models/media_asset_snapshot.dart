/// 媒体资产快照。
class MediaAssetSnapshot {
  const MediaAssetSnapshot({
    required this.assetId,
    required this.bizType,
    required this.mediaType,
    required this.fileName,
    required this.fileSize,
    required this.uploadStatus,
    required this.reviewStatus,
    required this.accessUrl,
    this.contentType,
    this.fileHash,
    this.completedAt,
    this.createdAt,
  });

  final String assetId;
  final String bizType;
  final String mediaType;
  final String fileName;
  final int fileSize;
  final String uploadStatus;
  final String reviewStatus;
  final String accessUrl;
  final String? contentType;
  final String? fileHash;
  final DateTime? completedAt;
  final DateTime? createdAt;
}
