import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';

class CitySelectionPage extends StatefulWidget {
  const CitySelectionPage({
    super.key,
    this.initialCityCode,
  });

  final String? initialCityCode;

  @override
  State<CitySelectionPage> createState() => _CitySelectionPageState();
}

typedef CitySelectionResult = ({String code, String name});

class _CitySelectionPageState extends State<CitySelectionPage> {
  late final TextEditingController _searchController;
  String _keyword = '';

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final List<_CityOption> visibleCities = _cityOptions
        .where(
          (_CityOption city) =>
              _keyword.trim().isEmpty ||
              city.name.contains(_keyword.trim()) ||
              city.code.contains(_keyword.trim()),
        )
        .toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('选择城市'),
      ),
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: <Color>[
              Color(0xFFFFFBF7),
              AppThemePalette.background,
            ],
          ),
        ),
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            CompanionCard(
              padding: const EdgeInsets.all(18),
              color: AppThemePalette.surface,
              radius: 24,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const CompanionPill(
                    label: '城市设置',
                    icon: Icons.location_city_rounded,
                    backgroundColor: Color(0xFFFFE2D2),
                    foregroundColor: AppThemePalette.primaryDeep,
                  ),
                  const SizedBox(height: 12),
                  Text('选一个常用城市',
                      style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 8),
                  Text(
                    '社区同城内容和后续服务中心都会以这里为默认城市。',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: AppThemePalette.muted,
                        ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _searchController,
                    decoration: const InputDecoration(
                      labelText: '搜索城市',
                      hintText: '例如 上海 / 杭州 / 成都',
                      prefixIcon: Icon(Icons.search_rounded),
                    ),
                    onChanged: (String value) {
                      setState(() {
                        _keyword = value;
                      });
                    },
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            if (visibleCities.isEmpty)
              const CompanionEmptyState(
                title: '没有找到匹配的城市',
                description: '可以换一个关键词试试，目前先从常用城市里选择。',
                icon: Icons.search_off_rounded,
              )
            else
              ...visibleCities.map(
                (_CityOption city) => Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: _CityCard(
                    city: city,
                    selected: city.code == widget.initialCityCode,
                    onTap: () => Navigator.of(context).pop(
                      (code: city.code, name: city.name),
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _CityCard extends StatelessWidget {
  const _CityCard({
    required this.city,
    required this.selected,
    required this.onTap,
  });

  final _CityOption city;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(22),
      child: Ink(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: selected
              ? const Color(0xFFFFECDD)
              : AppThemePalette.surfaceRaised,
          borderRadius: BorderRadius.circular(22),
        ),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: AppThemePalette.surface,
                borderRadius: BorderRadius.circular(16),
              ),
              child: const Icon(
                Icons.location_on_outlined,
                color: AppThemePalette.primaryDeep,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(city.name,
                      style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 4),
                  Text(
                    city.code,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: AppThemePalette.muted,
                        ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            if (selected)
              const CompanionPill(
                label: '当前选择',
                backgroundColor: AppThemePalette.surface,
              )
            else
              const Icon(Icons.chevron_right_rounded,
                  color: AppThemePalette.muted),
          ],
        ),
      ),
    );
  }
}

class _CityOption {
  const _CityOption({
    required this.code,
    required this.name,
  });

  final String code;
  final String name;
}

const List<_CityOption> _cityOptions = <_CityOption>[
  _CityOption(code: '310000', name: '上海'),
  _CityOption(code: '110000', name: '北京'),
  _CityOption(code: '440300', name: '深圳'),
  _CityOption(code: '440100', name: '广州'),
  _CityOption(code: '330100', name: '杭州'),
  _CityOption(code: '320100', name: '南京'),
  _CityOption(code: '320500', name: '苏州'),
  _CityOption(code: '420100', name: '武汉'),
  _CityOption(code: '510100', name: '成都'),
  _CityOption(code: '610100', name: '西安'),
];
