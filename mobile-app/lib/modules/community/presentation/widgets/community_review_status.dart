import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/shared/domain/models/community_post_snapshot.dart';
import 'package:petlife_mobile_app/shared/network/api_exception.dart';

bool isCommunityPostPendingReview(CommunityPostSnapshot post) {
  return post.reviewStatus == 'pending_review';
}

bool isCommunityPostRejected(CommunityPostSnapshot post) {
  return post.reviewStatus == 'rejected';
}

String communityReviewStatusLabel(String reviewStatus) {
  return switch (reviewStatus) {
    'pending_review' => '审核中',
    'rejected' => '审核未通过',
    'approved' => '已公开',
    _ => reviewStatus,
  };
}

String communityReviewStatusMessage(String reviewStatus) {
  return switch (reviewStatus) {
    'pending_review' => '已提交审核',
    'rejected' => '审核未通过',
    _ => '内容不可见',
  };
}

String communityContentUnavailableMessage(
  Object error, {
  required String contentLabel,
}) {
  if (error is ApiException) {
    switch (error.responseCode) {
      case 'COMMUNITY_POST_NOT_FOUND':
      case 'COMMUNITY_QUESTION_NOT_FOUND':
        return '$contentLabel不可见';
    }
  }
  return error.toString();
}

CompanionFeedbackTone communityReviewFeedbackTone(String reviewStatus) {
  return switch (reviewStatus) {
    'pending_review' => CompanionFeedbackTone.warning,
    'rejected' => CompanionFeedbackTone.error,
    _ => CompanionFeedbackTone.success,
  };
}

class CommunityReviewStatusNotice extends StatelessWidget {
  const CommunityReviewStatusNotice({
    super.key,
    required this.reviewStatus,
  });

  final String reviewStatus;

  @override
  Widget build(BuildContext context) {
    if (reviewStatus == 'approved') {
      return const SizedBox.shrink();
    }
    return CompanionFormNotice(
      message: communityReviewStatusMessage(reviewStatus),
      tone: communityReviewFeedbackTone(reviewStatus),
    );
  }
}

class CommunityReviewStatusPill extends StatelessWidget {
  const CommunityReviewStatusPill({
    super.key,
    required this.reviewStatus,
  });

  final String reviewStatus;

  @override
  Widget build(BuildContext context) {
    if (reviewStatus == 'approved') {
      return const SizedBox.shrink();
    }
    return CompanionPill(
      label: communityReviewStatusLabel(reviewStatus),
      icon: reviewStatus == 'pending_review'
          ? Icons.hourglass_top_rounded
          : Icons.visibility_off_outlined,
      backgroundColor: reviewStatus == 'pending_review'
          ? const Color(0xFFFFF3D7)
          : const Color(0xFFFFECE6),
      foregroundColor: reviewStatus == 'pending_review'
          ? const Color(0xFF8B6C20)
          : AppThemePalette.danger,
    );
  }
}
