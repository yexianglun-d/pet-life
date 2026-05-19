import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';

/// 社区作者关注按钮。
class CommunityAuthorFollowButton extends StatefulWidget {
  const CommunityAuthorFollowButton({
    super.key,
    required this.author,
  });

  final CommunityAuthorSnapshot author;

  @override
  State<CommunityAuthorFollowButton> createState() =>
      _CommunityAuthorFollowButtonState();
}

class _CommunityAuthorFollowButtonState
    extends State<CommunityAuthorFollowButton> {
  bool _didLoad = false;
  bool _isLoading = false;
  bool _isUpdating = false;
  bool _isSelf = false;
  CommunityFollowStatusSnapshot? _status;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_didLoad) {
      return;
    }
    _didLoad = true;
    _loadStatus();
  }

  @override
  void didUpdateWidget(covariant CommunityAuthorFollowButton oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.author.userId == widget.author.userId) {
      return;
    }
    _status = null;
    _isSelf = false;
    _loadStatus();
  }

  Future<void> _loadStatus() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CurrentUserSnapshot currentUser = await repository.getCurrentUser();
      if (currentUser.userId == widget.author.userId) {
        if (!mounted) {
          return;
        }
        setState(() {
          _isSelf = true;
          _status = null;
        });
        return;
      }

      final CommunityFollowStatusSnapshot status =
          await repository.getCommunityFollowStatus(widget.author.userId);
      if (!mounted) {
        return;
      }
      setState(() {
        _isSelf = false;
        _status = status;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _toggleFollow() async {
    final CommunityFollowStatusSnapshot? status = _status;
    if (status == null || _isUpdating) {
      return;
    }

    setState(() {
      _isUpdating = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final CommunityFollowStatusSnapshot updatedStatus = status.following
          ? await repository.unfollowCommunityUser(widget.author.userId)
          : await repository.followCommunityUser(widget.author.userId);
      if (!mounted) {
        return;
      }
      setState(() {
        _status = updatedStatus;
      });
      showCompanionSuccessFeedback(
        context,
        updatedStatus.following ? '已关注 ${widget.author.nickname}' : '已取消关注',
      );
    } catch (error) {
      if (!mounted) {
        return;
      }
      showCompanionErrorFeedback(context, error.toString());
    } finally {
      if (mounted) {
        setState(() {
          _isUpdating = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isSelf) {
      return const CompanionPill(
        label: '自己的分享',
        icon: Icons.account_circle_outlined,
        backgroundColor: AppThemePalette.surface,
      );
    }

    if (_isLoading && _status == null) {
      return OutlinedButton.icon(
        onPressed: null,
        icon: const Icon(Icons.hourglass_top_rounded),
        label: const Text('关注状态'),
      );
    }

    final CommunityFollowStatusSnapshot? status = _status;
    if (status == null) {
      return OutlinedButton.icon(
        onPressed: _loadStatus,
        icon: const Icon(Icons.refresh_rounded),
        label: const Text('重试关注'),
      );
    }

    return FilledButton.tonalIcon(
      onPressed: _isUpdating ? null : _toggleFollow,
      icon: Icon(
        status.following
            ? Icons.person_remove_alt_1_outlined
            : Icons.person_add_alt_1_outlined,
      ),
      label: Text(
        _isUpdating
            ? '处理中...'
            : status.following
                ? '取消关注'
                : '关注',
      ),
    );
  }
}
