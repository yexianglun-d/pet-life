import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/config/app_api_config.dart';
import 'package:petlife_mobile_app/shared/domain/models/media_asset_snapshot.dart';
import 'package:video_player/video_player.dart';

/// 社区内容媒体预览网格。
class CommunityMediaPreviewGrid extends StatefulWidget {
  const CommunityMediaPreviewGrid({
    super.key,
    required this.mediaAssets,
    this.mediaAssetIds = const <String>[],
    this.compact = false,
  });

  final List<MediaAssetSnapshot> mediaAssets;
  final List<String> mediaAssetIds;
  final bool compact;

  @override
  State<CommunityMediaPreviewGrid> createState() =>
      _CommunityMediaPreviewGridState();
}

class _CommunityMediaPreviewGridState extends State<CommunityMediaPreviewGrid> {
  bool _didLoadDependencies = false;
  String? _accessToken;

  Map<String, String> get _mediaHeaders {
    final String? accessToken = _accessToken;
    if (accessToken == null || accessToken.isEmpty) {
      return const <String, String>{};
    }
    return <String, String>{'Authorization': 'Bearer $accessToken'};
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoadDependencies) {
      return;
    }
    _didLoadDependencies = true;
    _loadAccessToken();
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

  void _openPreview(MediaAssetSnapshot asset) {
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
                        asset.fileName,
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
                if (asset.mediaType == 'video')
                  _CommunityVideoPreview(
                    uri: _resolveMediaUri(asset.accessUrl),
                    mediaHeaders: _mediaHeaders,
                  )
                else
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxHeight: 420),
                    child: InteractiveViewer(
                      child: Image.network(
                        _resolveMediaUri(asset.accessUrl).toString(),
                        headers: _mediaHeaders.isEmpty ? null : _mediaHeaders,
                        fit: BoxFit.contain,
                        errorBuilder: (_, __, ___) =>
                            const _CommunityMediaFallback(
                          icon: Icons.broken_image_outlined,
                          label: '图片暂时无法预览',
                        ),
                      ),
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
    if (widget.mediaAssets.isEmpty && widget.mediaAssetIds.isEmpty) {
      return const SizedBox.shrink();
    }

    if (widget.mediaAssets.isEmpty) {
      return _CommunityMediaPlaceholder(count: widget.mediaAssetIds.length);
    }

    final int maxVisible = widget.compact ? 3 : 9;
    final List<MediaAssetSnapshot> visibleAssets =
        widget.mediaAssets.take(maxVisible).toList();
    final int hiddenCount = widget.mediaAssets.length - visibleAssets.length;

    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: visibleAssets.indexed.map((item) {
        final int index = item.$1;
        final MediaAssetSnapshot asset = item.$2;
        final bool showHiddenCount =
            hiddenCount > 0 && index == visibleAssets.length - 1;
        return _CommunityMediaTile(
          asset: asset,
          compact: widget.compact,
          hiddenCount: showHiddenCount ? hiddenCount : 0,
          mediaHeaders: _mediaHeaders,
          onTap: () => _openPreview(asset),
        );
      }).toList(),
    );
  }
}

class _CommunityMediaTile extends StatelessWidget {
  const _CommunityMediaTile({
    required this.asset,
    required this.compact,
    required this.hiddenCount,
    required this.mediaHeaders,
    required this.onTap,
  });

  final MediaAssetSnapshot asset;
  final bool compact;
  final int hiddenCount;
  final Map<String, String> mediaHeaders;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final double size = compact ? 86 : 104;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(18),
        child: SizedBox(
          width: size,
          height: size,
          child: Stack(
            fit: StackFit.expand,
            children: [
              if (asset.mediaType == 'image')
                Image.network(
                  _resolveMediaUri(asset.accessUrl).toString(),
                  headers: mediaHeaders.isEmpty ? null : mediaHeaders,
                  fit: BoxFit.cover,
                  errorBuilder: (_, __, ___) => const _CommunityMediaFallback(
                    icon: Icons.broken_image_outlined,
                    label: '预览失败',
                  ),
                )
              else
                _CommunityMediaFallback(
                  icon: asset.mediaType == 'video'
                      ? Icons.play_circle_outline_rounded
                      : Icons.attach_file_rounded,
                  label: asset.mediaType == 'video' ? '视频' : '附件',
                ),
              if (asset.mediaType == 'video')
                const ColoredBox(color: Color(0x33000000)),
              if (asset.mediaType == 'video')
                const Center(
                  child: Icon(
                    Icons.play_circle_fill_rounded,
                    color: Colors.white,
                    size: 34,
                  ),
                ),
              if (hiddenCount > 0)
                ColoredBox(
                  color: const Color(0x66000000),
                  child: Center(
                    child: Text(
                      '+$hiddenCount',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: Colors.white,
                          ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CommunityMediaPlaceholder extends StatelessWidget {
  const _CommunityMediaPlaceholder({required this.count});

  final int count;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppThemePalette.surfaceRaised,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AppThemePalette.line),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.photo_library_outlined,
            color: AppThemePalette.primaryDeep,
          ),
          const SizedBox(width: 10),
          Text(
            '包含 $count 个媒体附件',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}

class _CommunityMediaFallback extends StatelessWidget {
  const _CommunityMediaFallback({
    required this.icon,
    required this.label,
  });

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppThemePalette.warmTint,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: AppThemePalette.primaryDeep, size: 30),
          const SizedBox(height: 6),
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

class _CommunityVideoPreview extends StatefulWidget {
  const _CommunityVideoPreview({
    required this.uri,
    required this.mediaHeaders,
  });

  final Uri uri;
  final Map<String, String> mediaHeaders;

  @override
  State<_CommunityVideoPreview> createState() => _CommunityVideoPreviewState();
}

class _CommunityVideoPreviewState extends State<_CommunityVideoPreview> {
  late final VideoPlayerController _controller;
  bool _isInitialized = false;
  bool _hasError = false;

  @override
  void initState() {
    super.initState();
    _controller = VideoPlayerController.networkUrl(
      widget.uri,
      httpHeaders: widget.mediaHeaders,
    );
    _controller.initialize().then((_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _isInitialized = true;
      });
    }).catchError((_) {
      if (!mounted) {
        return;
      }
      setState(() {
        _hasError = true;
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
    if (_hasError) {
      return const _CommunityMediaFallback(
        icon: Icons.play_disabled_outlined,
        label: '视频暂时无法预览',
      );
    }

    if (!_isInitialized) {
      return const _CommunityMediaFallback(
        icon: Icons.play_circle_outline_rounded,
        label: '视频准备中',
      );
    }

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(18),
          child: AspectRatio(
            aspectRatio: _controller.value.aspectRatio,
            child: VideoPlayer(_controller),
          ),
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

Uri _resolveMediaUri(String accessUrl) {
  final Uri uri = Uri.parse(accessUrl);
  if (uri.hasScheme) {
    return uri;
  }
  return AppApiConfig.baseUri.resolve(accessUrl);
}
