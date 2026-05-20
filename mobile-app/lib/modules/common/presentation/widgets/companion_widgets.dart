import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';

/// 轻盈圆润的陪伴式卡片容器。
class CompanionCard extends StatelessWidget {
  const CompanionCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(20),
    this.color = AppThemePalette.surface,
    this.gradient,
    this.radius = 28,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final Color color;
  final Gradient? gradient;
  final double radius;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: gradient == null ? color : null,
        gradient: gradient,
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: AppThemePalette.line),
        boxShadow: AppThemePalette.softShadow,
      ),
      child: child,
    );
  }
}

/// 统一的柔和标签。
class CompanionPill extends StatelessWidget {
  const CompanionPill({
    super.key,
    required this.label,
    this.icon,
    this.backgroundColor = AppThemePalette.warmTint,
    this.foregroundColor = AppThemePalette.title,
  });

  final String label;
  final IconData? icon;
  final Color backgroundColor;
  final Color foregroundColor;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 14, color: foregroundColor),
            const SizedBox(width: 6),
          ],
          Text(
            label,
            style: textTheme.bodySmall?.copyWith(
              color: foregroundColor,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

/// 陪伴型空状态。
class CompanionEmptyState extends StatelessWidget {
  const CompanionEmptyState({
    super.key,
    required this.title,
    required this.description,
    required this.icon,
    this.actionLabel,
    this.onAction,
  });

  final String title;
  final String description;
  final IconData icon;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppThemePalette.surfaceRaised,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: AppThemePalette.warmTint,
              borderRadius: BorderRadius.circular(18),
            ),
            child: Icon(icon, color: AppThemePalette.primaryDeep),
          ),
          const SizedBox(height: 14),
          Text(title, style: textTheme.titleMedium),
          if (actionLabel != null && onAction != null) ...[
            const SizedBox(height: 16),
            FilledButton.tonal(
              onPressed: onAction,
              child: Text(actionLabel!),
            ),
          ],
        ],
      ),
    );
  }
}
