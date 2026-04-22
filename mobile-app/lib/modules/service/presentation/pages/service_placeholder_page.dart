import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';

class ServicePlaceholderPage extends StatelessWidget {
  const ServicePlaceholderPage({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: const [
        _ServiceHeroSection(),
        SizedBox(height: 16),
        PageSection(
          title: '即将整理进来的照护服务',
          description: '把线下照护安排得更清楚，出门前也能更放心。',
          child: _ServicePreviewSection(),
        ),
        SizedBox(height: 16),
        PageSection(
          title: '服务中心会负责什么',
          description: '这里主要承接照护和预约，不会混进购物体验。',
          child: _ServiceBoundarySection(),
        ),
      ],
    );
  }
}

class _ServiceHeroSection extends StatelessWidget {
  const _ServiceHeroSection();

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      padding: const EdgeInsets.all(22),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: <Color>[
          Color(0xFFFFECDD),
          Color(0xFFFFFAF4),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: const [
          CompanionPill(
            label: '服务中心',
            icon: Icons.medical_services_outlined,
            backgroundColor: Color(0xFFFFE0CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          SizedBox(height: 12),
          Text('把照护安排得更安心'),
          SizedBox(height: 10),
          Text(
            '医院、洗护、寄养和训练都会在这里慢慢整理好，让日常照顾不再东一块西一块。',
          ),
        ],
      ),
    );
  }
}

class _ServicePreviewSection extends StatelessWidget {
  const _ServicePreviewSection();

  @override
  Widget build(BuildContext context) {
    return const Column(
      children: [
        _ServicePreviewCard(
          title: '宠物医院预约',
          description: '把体检、疫苗、复诊和医院联系信息收在同一个地方。',
          icon: Icons.local_hospital_outlined,
        ),
        SizedBox(height: 12),
        _ServicePreviewCard(
          title: '洗护美容',
          description: '洗澡、美容和回访提醒会一起整理，不容易忘记周期。',
          icon: Icons.bathtub_outlined,
        ),
        SizedBox(height: 12),
        _ServicePreviewCard(
          title: '寄养照看',
          description: '出门前把喂养说明、接送安排和注意事项交代清楚。',
          icon: Icons.house_siding_outlined,
        ),
        SizedBox(height: 12),
        _ServicePreviewCard(
          title: '训练服务',
          description: '记录训练目标、行为反馈和每次小进步，成长会更清楚。',
          icon: Icons.school_outlined,
        ),
      ],
    );
  }
}

class _ServicePreviewCard extends StatelessWidget {
  const _ServicePreviewCard({
    required this.title,
    required this.description,
    required this.icon,
  });

  final String title;
  final String description;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      radius: 22,
      color: AppThemePalette.surfaceRaised,
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: AppThemePalette.warmTint,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(icon, color: AppThemePalette.primaryDeep),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 6),
                Text(
                  description,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: AppThemePalette.body,
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

class _ServiceBoundarySection extends StatelessWidget {
  const _ServiceBoundarySection();

  @override
  Widget build(BuildContext context) {
    return const CompanionEmptyState(
      title: '商城会保持单独的体验边界',
      description: '服务中心只负责照护、预约和进度整理，商品购买不会混进这里。',
      icon: Icons.storefront_outlined,
    );
  }
}
