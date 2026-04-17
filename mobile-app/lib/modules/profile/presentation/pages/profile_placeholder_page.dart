import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';

class ProfilePlaceholderPage extends StatelessWidget {
  const ProfilePlaceholderPage({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: const [
        PageSection(
          title: '我的',
          description: '后续会补账户信息、家庭共养、收藏记录和设置中心。',
          child: Text('当前先保留结构入口，待联调登录态后继续展开。'),
        ),
      ],
    );
  }
}
