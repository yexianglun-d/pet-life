import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';

enum CompanionLoadingLayout {
  list,
  detail,
  compact,
}

/// 陪伴型页面加载态。
class CompanionPageLoading extends StatelessWidget {
  const CompanionPageLoading({
    super.key,
    required this.title,
    required this.description,
    this.icon = Icons.pets_rounded,
    this.layout = CompanionLoadingLayout.list,
    this.itemCount = 3,
    this.padding = const EdgeInsets.all(16),
  });

  final String title;
  final String description;
  final IconData icon;
  final CompanionLoadingLayout layout;
  final int itemCount;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: padding,
      children: [
        CompanionLoadingState(
          title: title,
          description: description,
          icon: icon,
        ),
        const SizedBox(height: 16),
        switch (layout) {
          CompanionLoadingLayout.list => CompanionSkeletonList(
              itemCount: itemCount,
            ),
          CompanionLoadingLayout.detail => const _DetailSkeleton(),
          CompanionLoadingLayout.compact => const CompanionSkeletonCard(
              lineCount: 2,
              showAvatar: false,
            ),
        },
      ],
    );
  }
}

/// 局部区域加载态，适合卡片、分区和短列表。
class CompanionLoadingState extends StatelessWidget {
  const CompanionLoadingState({
    super.key,
    required this.title,
    required this.description,
    this.icon = Icons.pets_rounded,
    this.compact = false,
  });

  final String title;
  final String description;
  final IconData icon;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    return CompanionCard(
      padding: EdgeInsets.all(compact ? 16 : 20),
      radius: compact ? 22 : 28,
      color: AppThemePalette.surface,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: compact ? 44 : 52,
            height: compact ? 44 : 52,
            decoration: BoxDecoration(
              color: AppThemePalette.warmTint,
              borderRadius: BorderRadius.circular(compact ? 16 : 18),
            ),
            child: Icon(icon, color: AppThemePalette.primaryDeep),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const CompanionPill(
                  label: '正在整理',
                  icon: Icons.auto_awesome_rounded,
                  backgroundColor: Color(0xFFFFE8D8),
                  foregroundColor: AppThemePalette.primaryDeep,
                ),
                const SizedBox(height: 10),
                Text(title, style: textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(
                  description,
                  style: textTheme.bodyMedium?.copyWith(
                    color: AppThemePalette.muted,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class CompanionSkeletonList extends StatelessWidget {
  const CompanionSkeletonList({
    super.key,
    this.itemCount = 3,
    this.showAvatar = true,
  });

  final int itemCount;
  final bool showAvatar;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: List<Widget>.generate(
        itemCount,
        (int index) => Padding(
          padding: EdgeInsets.only(bottom: index == itemCount - 1 ? 0 : 12),
          child: CompanionSkeletonCard(showAvatar: showAvatar),
        ),
      ),
    );
  }
}

class CompanionSkeletonCard extends StatelessWidget {
  const CompanionSkeletonCard({
    super.key,
    this.lineCount = 3,
    this.showAvatar = true,
  });

  final int lineCount;
  final bool showAvatar;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(16),
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (showAvatar) ...[
            const _SkeletonBox(width: 48, height: 48, radius: 18),
            const SizedBox(width: 12),
          ],
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: List<Widget>.generate(lineCount, (int index) {
                final bool isLast = index == lineCount - 1;
                return Padding(
                  padding: EdgeInsets.only(bottom: isLast ? 0 : 10),
                  child: _SkeletonBox(
                    widthFactor: index == 0
                        ? 0.62
                        : isLast
                            ? 0.44
                            : 0.86,
                    height: index == 0 ? 16 : 12,
                    radius: 999,
                  ),
                );
              }),
            ),
          ),
        ],
      ),
    );
  }
}

class _DetailSkeleton extends StatelessWidget {
  const _DetailSkeleton();

  @override
  Widget build(BuildContext context) {
    return const Column(
      children: [
        CompanionSkeletonCard(lineCount: 4, showAvatar: false),
        SizedBox(height: 12),
        CompanionSkeletonCard(lineCount: 3),
        SizedBox(height: 12),
        CompanionSkeletonCard(lineCount: 3, showAvatar: false),
      ],
    );
  }
}

class _SkeletonBox extends StatelessWidget {
  const _SkeletonBox({
    this.width,
    this.widthFactor,
    required this.height,
    required this.radius,
  });

  final double? width;
  final double? widthFactor;
  final double height;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final Widget box = Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: AppThemePalette.line.withValues(alpha: 0.75),
        borderRadius: BorderRadius.circular(radius),
      ),
    );

    if (widthFactor == null) {
      return box;
    }
    return FractionallySizedBox(
      widthFactor: widthFactor,
      alignment: Alignment.centerLeft,
      child: box,
    );
  }
}
