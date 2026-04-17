import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';

class ServicePlaceholderPage extends StatelessWidget {
  const ServicePlaceholderPage({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: const [
        PageSection(
          title: '服务',
          description: '当前阶段服务中心会优先落医院、寄养、洗护、训练目录与预约能力。',
          child: Text('商城当前只保留真实占位页，不进入后端交易链路。'),
        ),
      ],
    );
  }
}
