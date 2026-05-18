import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/config/app_api_config.dart';
import 'package:petlife_mobile_app/shared/domain/models/media_asset_snapshot.dart';
import 'package:video_player/video_player.dart';

/// 媒体附件选择状态。
class MediaAttachmentSelectionState {
  const MediaAttachmentSelectionState({
    required this.assetIds,
    required this.isUploading,
    required this.hasFailed,
  });

  final List<String> assetIds;
  final bool isUploading;
  final bool hasFailed;
}

/// 表单内媒体附件选择、上传、预览和删除组件。
class MediaAttachmentPicker extends StatefulWidget {
  const MediaAttachmentPicker({
    super.key,
    required this.bizType,
    required this.initialAssetIds,
    required this.allowedExtensions,
    required this.pickButtonLabel,
    required this.emptyDescription,
    required this.onSelectionChanged,
    this.maxItems = 9,
  });

  final String bizType;
  final List<String> initialAssetIds;
  final List<String> allowedExtensions;
  final String pickButtonLabel;
  final String emptyDescription;
  final int maxItems;
  final ValueChanged<MediaAttachmentSelectionState> onSelectionChanged;

  @override
  State<MediaAttachmentPicker> createState() => _MediaAttachmentPickerState();
}

class _MediaAttachmentPickerState extends State<MediaAttachmentPicker> {
  late final List<_PickedMediaAsset> _items;
  int _nextClientId = 0;
  bool _didLoadDependencies = false;
  String? _accessToken;

  @override
  void initState() {
    super.initState();
    _items = widget.initialAssetIds
        .map(_PickedMediaAsset.fromInitialAssetId)
        .toList(growable: true);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoadDependencies) {
      return;
    }
    _didLoadDependencies = true;
    _loadAccessToken();
    _loadInitialMediaAssets();
  }

  Map<String, String> get _mediaHeaders {
    final String? accessToken = _accessToken;
    if (accessToken == null || accessToken.isEmpty) {
      return const <String, String>{};
    }
    return <String, String>{'Authorization': 'Bearer $accessToken'};
  }

  Future<void> _loadAccessToken() async {
    final String? accessToken =
        await PetLifeAppScope.sessionStoreOf(context).readAccessToken();
    if (!mounted) {
      return;
    }
    setState(() {
      _accessToken = accessToken;
    });
  }

  Future<void> _loadInitialMediaAssets() async {
    if (widget.initialAssetIds.isEmpty) {
      return;
    }
    final repository = PetLifeAppScope.repositoryOf(context);
    for (final String assetId in widget.initialAssetIds) {
      try {
        final MediaAssetSnapshot mediaAsset =
            await repository.getMediaAsset(assetId);
        if (!mounted) {
          return;
        }
        _replaceItem(
          'remote-$assetId',
          (_) => _PickedMediaAsset.fromMediaAsset(mediaAsset),
        );
      } catch (_) {
        // 旧记录的 asset_id 仍随表单保留；元数据加载失败时不阻断用户编辑。
      }
    }
  }

  Future<void> _pickFiles() async {
    final int remainingCount = widget.maxItems - _items.length;
    if (remainingCount <= 0) {
      _showMessage('最多只能添加 ${widget.maxItems} 个附件');
      return;
    }

    final FilePickerResult? result = await FilePicker.pickFiles(
      allowMultiple: remainingCount > 1,
      type: FileType.custom,
      allowedExtensions: widget.allowedExtensions,
      withData: false,
    );
    if (!mounted || result == null) {
      return;
    }

    final List<PlatformFile> files =
        result.files.where((PlatformFile file) => file.path != null).toList();
    if (files.isEmpty) {
      _showMessage('没有读取到可上传的文件');
      return;
    }

    for (final PlatformFile file in files.take(remainingCount)) {
      final _PickedMediaAsset item = _PickedMediaAsset.uploading(
        clientId: 'local-${_nextClientId++}',
        file: file,
      );
      setState(() {
        _items.add(item);
      });
      _emitSelectionState();
      _uploadItem(item.clientId, file.path!);
    }
  }

  Future<void> _uploadItem(String clientId, String filePath) async {
    try {
      final MediaAssetSnapshot mediaAsset =
          await PetLifeAppScope.repositoryOf(context).uploadMediaAsset(
        bizType: widget.bizType,
        filePath: filePath,
      );
      if (!mounted) {
        return;
      }
      _replaceItem(
        clientId,
        (item) => item.toUploaded(mediaAsset),
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      _replaceItem(
        clientId,
        (item) => item.toFailed(error.toString()),
      );
    }
  }

  void _replaceItem(
    String clientId,
    _PickedMediaAsset Function(_PickedMediaAsset item) mapper,
  ) {
    final int index = _items
        .indexWhere((_PickedMediaAsset item) => item.clientId == clientId);
    if (index < 0) {
      return;
    }
    setState(() {
      _items[index] = mapper(_items[index]);
    });
    _emitSelectionState();
  }

  void _removeItem(String clientId) {
    setState(() {
      _items.removeWhere(
        (_PickedMediaAsset item) => item.clientId == clientId,
      );
    });
    _emitSelectionState();
  }

  void _retryItem(String clientId) {
    final int index = _items
        .indexWhere((_PickedMediaAsset item) => item.clientId == clientId);
    if (index < 0) {
      return;
    }
    final _PickedMediaAsset item = _items[index];
    final String? localPath = item.localPath;
    if (localPath == null) {
      _showMessage('这个附件缺少本地文件，请重新选择');
      return;
    }

    setState(() {
      _items[index] = item.toRetrying();
    });
    _emitSelectionState();
    _uploadItem(clientId, localPath);
  }

  void _emitSelectionState() {
    widget.onSelectionChanged(
      MediaAttachmentSelectionState(
        assetIds: _items
            .where((_PickedMediaAsset item) => item.isUploaded)
            .map((_PickedMediaAsset item) => item.assetId!)
            .toList(),
        isUploading: _items.any((_PickedMediaAsset item) => item.isUploading),
        hasFailed: _items.any((_PickedMediaAsset item) => item.isFailed),
      ),
    );
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  void _openPreview(_PickedMediaAsset item) {
    showDialog<void>(
      context: context,
      builder: (BuildContext context) {
        return Dialog(
          insetPadding: const EdgeInsets.all(18),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        item.fileName,
                        style: Theme.of(context).textTheme.titleMedium,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.close_rounded),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                _MediaPreview(
                  item: item,
                  expanded: true,
                  mediaHeaders: _mediaHeaders,
                ),
                const SizedBox(height: 12),
                Text(
                  _buildItemSubtitle(item),
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppThemePalette.muted,
                      ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        OutlinedButton.icon(
          onPressed: _pickFiles,
          icon: const Icon(Icons.add_photo_alternate_outlined),
          label: Text(widget.pickButtonLabel),
        ),
        const SizedBox(height: 12),
        if (_items.isEmpty)
          _EmptyMediaBox(description: widget.emptyDescription)
        else
          ..._items.map(
            (_PickedMediaAsset item) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _MediaAttachmentTile(
                item: item,
                subtitle: _buildItemSubtitle(item),
                mediaHeaders: _mediaHeaders,
                onPreview: () => _openPreview(item),
                onRetry: item.isFailed ? () => _retryItem(item.clientId) : null,
                onRemove: () => _removeItem(item.clientId),
              ),
            ),
          ),
      ],
    );
  }
}

class _MediaAttachmentTile extends StatelessWidget {
  const _MediaAttachmentTile({
    required this.item,
    required this.subtitle,
    required this.mediaHeaders,
    required this.onPreview,
    required this.onRetry,
    required this.onRemove,
  });

  final _PickedMediaAsset item;
  final String subtitle;
  final Map<String, String> mediaHeaders;
  final VoidCallback onPreview;
  final VoidCallback? onRetry;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Material(
      color: AppThemePalette.surfaceRaised,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: onPreview,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: SizedBox(
                  width: 72,
                  height: 72,
                  child: _MediaPreview(
                    item: item,
                    expanded: false,
                    mediaHeaders: mediaHeaders,
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.fileName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: textTheme.titleMedium,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      subtitle,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: textTheme.bodySmall,
                    ),
                    if (item.isUploading) ...[
                      const SizedBox(height: 8),
                      const LinearProgressIndicator(minHeight: 4),
                    ],
                    if (item.isFailed) ...[
                      const SizedBox(height: 8),
                      Text(
                        item.errorMessage ?? '上传失败',
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: textTheme.bodySmall?.copyWith(
                          color: AppThemePalette.danger,
                        ),
                      ),
                      if (onRetry != null) ...[
                        const SizedBox(height: 6),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: TextButton.icon(
                            onPressed: onRetry,
                            icon: const Icon(Icons.refresh_rounded, size: 16),
                            label: const Text('重新上传'),
                          ),
                        ),
                      ],
                    ],
                  ],
                ),
              ),
              IconButton(
                tooltip: '移除',
                onPressed: onRemove,
                icon: const Icon(Icons.delete_outline_rounded),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MediaPreview extends StatelessWidget {
  const _MediaPreview({
    required this.item,
    required this.expanded,
    required this.mediaHeaders,
  });

  final _PickedMediaAsset item;
  final bool expanded;
  final Map<String, String> mediaHeaders;

  @override
  Widget build(BuildContext context) {
    final String? localPath = item.localPath;
    if (localPath != null && item.mediaType == 'image') {
      final Widget image = Image.file(
        File(localPath),
        fit: expanded ? BoxFit.contain : BoxFit.cover,
        errorBuilder: (_, __, ___) => _FileTypePreview(
          icon: Icons.broken_image_outlined,
          label: '图片预览失败',
          expanded: expanded,
        ),
      );
      return expanded
          ? ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 420),
              child: InteractiveViewer(child: image),
            )
          : image;
    }

    if (localPath != null && item.mediaType == 'video') {
      return _VideoPreview(
        filePath: localPath,
        expanded: expanded,
        mediaHeaders: mediaHeaders,
      );
    }

    final String? accessUrl = item.accessUrl;
    if (accessUrl != null && item.mediaType == 'image') {
      final Uri uri = _resolveMediaUri(accessUrl);
      final Widget image = Image.network(
        uri.toString(),
        headers: mediaHeaders.isEmpty ? null : mediaHeaders,
        fit: expanded ? BoxFit.contain : BoxFit.cover,
        errorBuilder: (_, __, ___) => _FileTypePreview(
          icon: Icons.broken_image_outlined,
          label: '图片预览失败',
          expanded: expanded,
        ),
      );
      return expanded
          ? ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 420),
              child: InteractiveViewer(child: image),
            )
          : image;
    }

    if (accessUrl != null && item.mediaType == 'video') {
      return _VideoPreview(
        remoteUri: _resolveMediaUri(accessUrl),
        expanded: expanded,
        mediaHeaders: mediaHeaders,
      );
    }

    return _FileTypePreview(
      icon: item.mediaType == 'file'
          ? Icons.picture_as_pdf_outlined
          : Icons.cloud_done_outlined,
      label: item.isUploaded ? '已上传' : _toLocalizedMediaType(item.mediaType),
      expanded: expanded,
    );
  }
}

class _VideoPreview extends StatefulWidget {
  const _VideoPreview({
    required this.expanded,
    required this.mediaHeaders,
    this.filePath,
    this.remoteUri,
  });

  final String? filePath;
  final Uri? remoteUri;
  final bool expanded;
  final Map<String, String> mediaHeaders;

  @override
  State<_VideoPreview> createState() => _VideoPreviewState();
}

class _VideoPreviewState extends State<_VideoPreview> {
  late final VideoPlayerController _controller;
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    final String? filePath = widget.filePath;
    final Uri? remoteUri = widget.remoteUri;
    if (filePath != null) {
      _controller = VideoPlayerController.file(File(filePath));
    } else {
      _controller = VideoPlayerController.networkUrl(
        remoteUri!,
        httpHeaders: widget.mediaHeaders,
      );
    }
    _controller.initialize().then((_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _isInitialized = true;
      });
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _togglePlayback() {
    if (!_isInitialized) {
      return;
    }
    setState(() {
      if (_controller.value.isPlaying) {
        _controller.pause();
      } else {
        _controller.play();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    if (!_isInitialized) {
      return _FileTypePreview(
        icon: Icons.play_circle_outline_rounded,
        label: '视频准备中',
        expanded: widget.expanded,
      );
    }

    final Widget video = AspectRatio(
      aspectRatio: _controller.value.aspectRatio,
      child: VideoPlayer(_controller),
    );

    if (!widget.expanded) {
      return Stack(
        fit: StackFit.expand,
        children: [
          FittedBox(
            fit: BoxFit.cover,
            child: SizedBox(
              width: _controller.value.size.width,
              height: _controller.value.size.height,
              child: video,
            ),
          ),
          const ColoredBox(color: Color(0x33000000)),
          const Center(
            child: Icon(
              Icons.play_circle_fill_rounded,
              color: Colors.white,
              size: 32,
            ),
          ),
        ],
      );
    }

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(18),
          child: video,
        ),
        const SizedBox(height: 12),
        FilledButton.tonalIcon(
          onPressed: _togglePlayback,
          icon: Icon(
            _controller.value.isPlaying
                ? Icons.pause_rounded
                : Icons.play_arrow_rounded,
          ),
          label: Text(_controller.value.isPlaying ? '暂停预览' : '播放预览'),
        ),
      ],
    );
  }
}

class _FileTypePreview extends StatelessWidget {
  const _FileTypePreview({
    required this.icon,
    required this.label,
    required this.expanded,
  });

  final IconData icon;
  final String label;
  final bool expanded;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      height: expanded ? 180 : double.infinity,
      color: AppThemePalette.warmTint,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            icon,
            color: AppThemePalette.primaryDeep,
            size: expanded ? 48 : 28,
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: Theme.of(context).textTheme.bodySmall,
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}

class _EmptyMediaBox extends StatelessWidget {
  const _EmptyMediaBox({required this.description});

  final String description;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppThemePalette.surfaceRaised,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: AppThemePalette.line),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.photo_library_outlined,
            color: AppThemePalette.primaryDeep,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              description,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.muted,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PickedMediaAsset {
  const _PickedMediaAsset({
    required this.clientId,
    required this.fileName,
    required this.mediaType,
    required this.uploadState,
    this.assetId,
    this.localPath,
    this.fileSize,
    this.accessUrl,
    this.errorMessage,
  });

  final String clientId;
  final String fileName;
  final String mediaType;
  final _UploadState uploadState;
  final String? assetId;
  final String? localPath;
  final int? fileSize;
  final String? accessUrl;
  final String? errorMessage;

  bool get isUploaded => uploadState == _UploadState.uploaded;

  bool get isUploading => uploadState == _UploadState.uploading;

  bool get isFailed => uploadState == _UploadState.failed;

  static _PickedMediaAsset fromInitialAssetId(String assetId) {
    return _PickedMediaAsset(
      clientId: 'remote-$assetId',
      assetId: assetId,
      fileName: '已上传附件 $assetId',
      mediaType: 'file',
      uploadState: _UploadState.uploaded,
    );
  }

  static _PickedMediaAsset fromMediaAsset(MediaAssetSnapshot mediaAsset) {
    return _PickedMediaAsset(
      clientId: 'remote-${mediaAsset.assetId}',
      assetId: mediaAsset.assetId,
      fileName: mediaAsset.fileName,
      mediaType: mediaAsset.mediaType,
      fileSize: mediaAsset.fileSize,
      accessUrl: mediaAsset.accessUrl,
      uploadState: _UploadState.uploaded,
    );
  }

  static _PickedMediaAsset uploading({
    required String clientId,
    required PlatformFile file,
  }) {
    return _PickedMediaAsset(
      clientId: clientId,
      fileName: file.name,
      mediaType: _resolveMediaType(file.name, file.extension),
      localPath: file.path,
      fileSize: file.size,
      uploadState: _UploadState.uploading,
    );
  }

  _PickedMediaAsset toUploaded(MediaAssetSnapshot mediaAsset) {
    return _PickedMediaAsset(
      clientId: clientId,
      assetId: mediaAsset.assetId,
      fileName: mediaAsset.fileName,
      mediaType: mediaAsset.mediaType,
      uploadState: _UploadState.uploaded,
      localPath: localPath,
      fileSize: mediaAsset.fileSize,
      accessUrl: mediaAsset.accessUrl,
    );
  }

  _PickedMediaAsset toFailed(String message) {
    return _PickedMediaAsset(
      clientId: clientId,
      fileName: fileName,
      mediaType: mediaType,
      uploadState: _UploadState.failed,
      localPath: localPath,
      fileSize: fileSize,
      errorMessage: message,
    );
  }

  _PickedMediaAsset toRetrying() {
    return _PickedMediaAsset(
      clientId: clientId,
      fileName: fileName,
      mediaType: mediaType,
      uploadState: _UploadState.uploading,
      localPath: localPath,
      fileSize: fileSize,
    );
  }
}

enum _UploadState {
  uploading,
  uploaded,
  failed,
}

String _buildItemSubtitle(_PickedMediaAsset item) {
  final String sizeLabel =
      item.fileSize == null ? '' : ' · ${_formatFileSize(item.fileSize!)}';
  if (item.isUploading) {
    return '${_toLocalizedMediaType(item.mediaType)}$sizeLabel · 上传中';
  }
  if (item.isFailed) {
    return '${_toLocalizedMediaType(item.mediaType)}$sizeLabel · 上传失败';
  }
  if (item.accessUrl != null) {
    return '${_toLocalizedMediaType(item.mediaType)}$sizeLabel · 已上传';
  }
  return '${_toLocalizedMediaType(item.mediaType)} · 已关联';
}

String _resolveMediaType(String fileName, String? extension) {
  final String normalizedExtension =
      (extension ?? _extensionOf(fileName)).toLowerCase().replaceFirst('.', '');
  if (<String>{'jpg', 'jpeg', 'png', 'webp', 'gif'}
      .contains(normalizedExtension)) {
    return 'image';
  }
  if (<String>{'mp4', 'mov'}.contains(normalizedExtension)) {
    return 'video';
  }
  return 'file';
}

String _extensionOf(String fileName) {
  final int dotIndex = fileName.lastIndexOf('.');
  if (dotIndex < 0 || dotIndex == fileName.length - 1) {
    return '';
  }
  return fileName.substring(dotIndex + 1);
}

String _toLocalizedMediaType(String mediaType) {
  switch (mediaType) {
    case 'image':
      return '图片';
    case 'video':
      return '视频';
    case 'file':
      return '文件';
    default:
      return mediaType;
  }
}

Uri _resolveMediaUri(String accessUrl) {
  final Uri uri = Uri.parse(accessUrl);
  if (uri.hasScheme) {
    return uri;
  }
  return AppApiConfig.baseUri.resolve(accessUrl);
}

String _formatFileSize(int bytes) {
  if (bytes >= 1024 * 1024) {
    return '${(bytes / 1024 / 1024).toStringAsFixed(1)} MB';
  }
  if (bytes >= 1024) {
    return '${(bytes / 1024).toStringAsFixed(1)} KB';
  }
  return '$bytes B';
}
