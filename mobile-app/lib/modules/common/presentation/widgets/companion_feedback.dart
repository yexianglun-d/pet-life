import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';

enum CompanionFeedbackTone {
  info,
  success,
  warning,
  error,
}

/// 陪伴式确认底部弹层。
Future<bool> showCompanionConfirmSheet(
  BuildContext context, {
  required String title,
  required String description,
  required String confirmLabel,
  String cancelLabel = '先取消',
  Color? confirmColor,
}) async {
  final bool? confirmed = await showModalBottomSheet<bool>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (BuildContext context) {
      return SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          child: CompanionCard(
            padding: const EdgeInsets.all(20),
            radius: 28,
            color: AppThemePalette.surface,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 10),
                Text(
                  description,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppThemePalette.body,
                      ),
                ),
                const SizedBox(height: 18),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    style: FilledButton.styleFrom(
                      backgroundColor:
                          confirmColor ?? AppThemePalette.primaryDeep,
                    ),
                    onPressed: () => Navigator.of(context).pop(true),
                    child: Text(confirmLabel),
                  ),
                ),
                const SizedBox(height: 10),
                SizedBox(
                  width: double.infinity,
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(false),
                    child: Text(cancelLabel),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    },
  );
  return confirmed ?? false;
}

void showCompanionSuccessFeedback(
  BuildContext context,
  String message,
) {
  showCompanionFeedback(
    context,
    message: message,
    tone: CompanionFeedbackTone.success,
  );
}

void showCompanionErrorFeedback(
  BuildContext context,
  String message,
) {
  showCompanionFeedback(
    context,
    message: message,
    tone: CompanionFeedbackTone.error,
  );
}

/// 统一的轻量反馈提示。
void showCompanionFeedback(
  BuildContext context, {
  required String message,
  CompanionFeedbackTone tone = CompanionFeedbackTone.info,
}) {
  final _CompanionFeedbackColors colors = _feedbackColors(tone);
  final TextTheme textTheme = Theme.of(context).textTheme;

  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        behavior: SnackBarBehavior.floating,
        backgroundColor: colors.background,
        elevation: 0,
        margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: BorderSide(color: colors.border),
        ),
        content: Row(
          children: [
            Icon(colors.icon, color: colors.foreground, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                message,
                style: textTheme.bodyMedium?.copyWith(
                  color: colors.foreground,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
      ),
    );
}

/// 表单顶部的统一校验提示块。
class CompanionFormNotice extends StatelessWidget {
  const CompanionFormNotice({
    super.key,
    required this.message,
    this.tone = CompanionFeedbackTone.error,
  });

  final String message;
  final CompanionFeedbackTone tone;

  @override
  Widget build(BuildContext context) {
    final _CompanionFeedbackColors colors = _feedbackColors(tone);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.background,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: colors.border),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(colors.icon, color: colors.foreground, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: colors.foreground,
                    fontWeight: FontWeight.w700,
                  ),
            ),
          ),
        ],
      ),
    );
  }
}

_CompanionFeedbackColors _feedbackColors(CompanionFeedbackTone tone) {
  switch (tone) {
    case CompanionFeedbackTone.success:
      return const _CompanionFeedbackColors(
        background: Color(0xFFEAF4EA),
        border: Color(0xFFBFD9C2),
        foreground: AppThemePalette.success,
        icon: Icons.check_circle_rounded,
      );
    case CompanionFeedbackTone.warning:
      return const _CompanionFeedbackColors(
        background: Color(0xFFFFF3D7),
        border: Color(0xFFE8CA7C),
        foreground: Color(0xFF8B6C20),
        icon: Icons.info_rounded,
      );
    case CompanionFeedbackTone.error:
      return const _CompanionFeedbackColors(
        background: Color(0xFFFFECE6),
        border: Color(0xFFE7BBB1),
        foreground: AppThemePalette.danger,
        icon: Icons.error_rounded,
      );
    case CompanionFeedbackTone.info:
      return const _CompanionFeedbackColors(
        background: Color(0xFFEAF5F8),
        border: Color(0xFFC5DDE4),
        foreground: Color(0xFF4C7885),
        icon: Icons.tips_and_updates_rounded,
      );
  }
}

class _CompanionFeedbackColors {
  const _CompanionFeedbackColors({
    required this.background,
    required this.border,
    required this.foreground,
    required this.icon,
  });

  final Color background;
  final Color border;
  final Color foreground;
  final IconData icon;
}
