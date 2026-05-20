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
          description: '',
          child: SizedBox.shrink(),
        ),
      ],
    );
  }
}
