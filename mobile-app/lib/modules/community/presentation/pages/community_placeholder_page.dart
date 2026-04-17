import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';

class CommunityPlaceholderPage extends StatelessWidget {
  const CommunityPlaceholderPage({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: const [
        PageSection(
          title: '社区',
          description: '当前进入社区主链路建设期，下一批会补推荐流、关注流和发布链路。',
          child: Text('已规划范围：推荐、关注、同城、问答、举报与审核。'),
        ),
      ],
    );
  }
}
