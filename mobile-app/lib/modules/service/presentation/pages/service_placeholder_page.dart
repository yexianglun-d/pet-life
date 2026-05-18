import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_widgets.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/page_section.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/current_user_snapshot.dart';
import 'package:petlife_mobile_app/shared/domain/models/service_center_snapshot.dart';

class ServicePlaceholderPage extends StatefulWidget {
  const ServicePlaceholderPage({super.key});

  @override
  State<ServicePlaceholderPage> createState() => _ServicePlaceholderPageState();
}

class _ServicePlaceholderPageState extends State<ServicePlaceholderPage> {
  Future<_ServiceHomeViewData>? _viewDataFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _viewDataFuture ??= _loadViewData();
  }

  Future<_ServiceHomeViewData> _loadViewData() async {
    final repository = PetLifeAppScope.repositoryOf(context);
    final CurrentUserSnapshot currentUser = await repository.getCurrentUser();
    final ServiceHomeSnapshot serviceHome = await repository.getServiceHome(
      petId: currentUser.currentPetId,
      cityCode: currentUser.cityCode,
    );
    return _ServiceHomeViewData(
      currentUser: currentUser,
      serviceHome: serviceHome,
    );
  }

  void _reload() {
    setState(() {
      _viewDataFuture = _loadViewData();
    });
  }

  Future<void> _openProviderList(ServiceCategorySnapshot category) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => ServiceProviderListPage(category: category),
      ),
    );
    if (mounted) {
      _reload();
    }
  }

  Future<void> _openProviderDetail(ServiceProviderSnapshot provider) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) =>
            ServiceProviderDetailPage(providerId: provider.providerId),
      ),
    );
    if (mounted) {
      _reload();
    }
  }

  Future<void> _openAppointments() async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => const ServiceAppointmentListPage(),
      ),
    );
    if (mounted) {
      _reload();
    }
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<_ServiceHomeViewData>(
      future: _viewDataFuture,
      builder:
          (BuildContext context, AsyncSnapshot<_ServiceHomeViewData> snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError || !snapshot.hasData) {
          return _ErrorState(
              message: snapshot.error.toString(), onRetry: _reload);
        }

        final ServiceHomeSnapshot serviceHome = snapshot.data!.serviceHome;
        return RefreshIndicator(
          onRefresh: () async => _reload(),
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _ServiceHeroSection(
                cityName: serviceHome.cityName,
                opened: serviceHome.opened,
                onAppointmentsTap: _openAppointments,
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '照护服务',
                description: serviceHome.opened
                    ? '按场景找到合适的服务商，再把预约和说明一次性整理好。'
                    : serviceHome.unavailableReason ?? '当前城市暂未开通服务。',
                child: _CategorySection(
                  categories: serviceHome.categories,
                  onTap: _openProviderList,
                ),
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '推荐服务商',
                description: '先看离你当前城市最近、评分和服务项目更完整的服务商。',
                child: serviceHome.featuredProviders.isEmpty
                    ? const CompanionEmptyState(
                        title: '当前城市还没有服务商',
                        description: '城市开通后，医院、洗护、寄养和训练会出现在这里。',
                        icon: Icons.location_city_outlined,
                      )
                    : _ProviderList(
                        providers: serviceHome.featuredProviders,
                        onTap: _openProviderDetail,
                      ),
              ),
              const SizedBox(height: 16),
              PageSection(
                title: '近期预约',
                description: '已提交的预约会在这里回看，也会同步进入消息和时间轴。',
                actionLabel: '查看全部',
                onAction: _openAppointments,
                child: _AppointmentPreviewList(
                  appointments: serviceHome.upcomingAppointments,
                ),
              ),
              const SizedBox(height: 16),
              CompanionEmptyState(
                title: '商城保持单独边界',
                description: serviceHome.commercePlaceholder,
                icon: Icons.storefront_outlined,
              ),
            ],
          ),
        );
      },
    );
  }
}

class ServiceProviderListPage extends StatefulWidget {
  const ServiceProviderListPage({
    super.key,
    required this.category,
  });

  final ServiceCategorySnapshot category;

  @override
  State<ServiceProviderListPage> createState() =>
      _ServiceProviderListPageState();
}

class _ServiceProviderListPageState extends State<ServiceProviderListPage> {
  late Future<List<ServiceProviderSnapshot>> _providersFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _providersFuture =
        PetLifeAppScope.repositoryOf(context).listServiceProviders(
      providerType: widget.category.providerType,
    );
  }

  Future<void> _openProvider(ServiceProviderSnapshot provider) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (_) =>
            ServiceProviderDetailPage(providerId: provider.providerId),
      ),
    );
    if (mounted) {
      setState(() {
        _providersFuture =
            PetLifeAppScope.repositoryOf(context).listServiceProviders(
          providerType: widget.category.providerType,
        );
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.category.title)),
      body: FutureBuilder<List<ServiceProviderSnapshot>>(
        future: _providersFuture,
        builder: (BuildContext context,
            AsyncSnapshot<List<ServiceProviderSnapshot>> snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError || !snapshot.hasData) {
            return _ErrorState(
              message: snapshot.error.toString(),
              onRetry: () {
                setState(() {
                  _providersFuture = PetLifeAppScope.repositoryOf(context)
                      .listServiceProviders(
                          providerType: widget.category.providerType);
                });
              },
            );
          }
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              PageSection(
                title: widget.category.title,
                description: widget.category.description,
                child: snapshot.data!.isEmpty
                    ? const CompanionEmptyState(
                        title: '暂时没有可预约服务商',
                        description: '可以稍后再看，或切换城市后重新查找。',
                        icon: Icons.search_off_rounded,
                      )
                    : _ProviderList(
                        providers: snapshot.data!,
                        onTap: _openProvider,
                      ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class ServiceProviderDetailPage extends StatefulWidget {
  const ServiceProviderDetailPage({
    super.key,
    required this.providerId,
  });

  final String providerId;

  @override
  State<ServiceProviderDetailPage> createState() =>
      _ServiceProviderDetailPageState();
}

class _ServiceProviderDetailPageState extends State<ServiceProviderDetailPage> {
  late Future<ServiceProviderSnapshot> _providerFuture;
  late Future<List<ProviderReviewSnapshot>> _reviewsFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _providerFuture = PetLifeAppScope.repositoryOf(context)
        .getServiceProvider(widget.providerId);
    _reviewsFuture = PetLifeAppScope.repositoryOf(context)
        .listProviderReviews(providerId: widget.providerId);
  }

  Future<void> _openAppointment(ServiceProviderSnapshot provider) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => ServiceAppointmentEditorPage(provider: provider),
      ),
    );
    if (!mounted || changed != true) {
      return;
    }
    setState(() {
      _providerFuture = PetLifeAppScope.repositoryOf(context)
          .getServiceProvider(widget.providerId);
      _reviewsFuture = PetLifeAppScope.repositoryOf(context)
          .listProviderReviews(providerId: widget.providerId);
    });
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<ServiceProviderSnapshot>(
      future: _providerFuture,
      builder: (BuildContext context,
          AsyncSnapshot<ServiceProviderSnapshot> snapshot) {
        final String title = snapshot.data?.providerName ?? '服务商详情';
        return Scaffold(
          appBar: AppBar(title: Text(title)),
          body: snapshot.connectionState != ConnectionState.done
              ? const Center(child: CircularProgressIndicator())
              : snapshot.hasError || !snapshot.hasData
                  ? _ErrorState(
                      message: snapshot.error.toString(),
                      onRetry: () {
                        setState(() {
                          _providerFuture =
                              PetLifeAppScope.repositoryOf(context)
                                  .getServiceProvider(widget.providerId);
                        });
                      },
                    )
                  : _ProviderDetailBody(
                      provider: snapshot.data!,
                      reviewsFuture: _reviewsFuture,
                      onAppointmentTap: () => _openAppointment(snapshot.data!),
                    ),
        );
      },
    );
  }
}

class ServiceAppointmentEditorPage extends StatefulWidget {
  const ServiceAppointmentEditorPage({
    super.key,
    required this.provider,
  });

  final ServiceProviderSnapshot provider;

  @override
  State<ServiceAppointmentEditorPage> createState() =>
      _ServiceAppointmentEditorPageState();
}

class _ServiceAppointmentEditorPageState
    extends State<ServiceAppointmentEditorPage> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _demandController = TextEditingController();
  final TextEditingController _contactNameController = TextEditingController();
  final TextEditingController _contactMobileController =
      TextEditingController();
  String? _selectedSlotId;
  String? _currentPetId;
  bool _loadingUser = true;
  bool _submitting = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _loadCurrentUser();
  }

  Future<void> _loadCurrentUser() async {
    final CurrentUserSnapshot currentUser =
        await PetLifeAppScope.repositoryOf(context).getCurrentUser();
    if (!mounted) {
      return;
    }
    setState(() {
      _currentPetId = currentUser.currentPetId;
      _contactNameController.text = currentUser.nickname;
      _contactMobileController.text = currentUser.mobile;
      _selectedSlotId = widget.provider.availableSlots.isEmpty
          ? null
          : widget.provider.availableSlots.first.slotId;
      _loadingUser = false;
    });
  }

  @override
  void dispose() {
    _demandController.dispose();
    _contactNameController.dispose();
    _contactMobileController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_submitting || !_formKey.currentState!.validate()) {
      return;
    }
    if (_currentPetId == null || _selectedSlotId == null) {
      showCompanionErrorFeedback(context, '当前没有可提交的宠物或预约时段');
      return;
    }
    final ProviderScheduleSlotSnapshot selectedSlot = widget
        .provider.availableSlots
        .firstWhere((ProviderScheduleSlotSnapshot slot) =>
            slot.slotId == _selectedSlotId);

    setState(() {
      _submitting = true;
    });
    try {
      await PetLifeAppScope.repositoryOf(context).createServiceAppointment(
        ServiceAppointmentDraft(
          petId: _currentPetId!,
          providerId: widget.provider.providerId,
          appointmentType: widget.provider.providerType,
          appointmentDate: selectedSlot.slotDate,
          appointmentSlot: selectedSlot.displayText,
          demandDesc: _normalizeNullableText(_demandController.text),
          contactName: _contactNameController.text.trim(),
          contactMobile: _contactMobileController.text.trim(),
        ),
      );
      if (!mounted) {
        return;
      }
      showCompanionSuccessFeedback(context, '预约已经提交，等待服务方确认');
      Navigator.of(context).pop(true);
    } catch (error) {
      if (mounted) {
        showCompanionErrorFeedback(context, error.toString());
      }
    } finally {
      if (mounted) {
        setState(() {
          _submitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('提交预约')),
      body: _loadingUser
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _ProviderCard(provider: widget.provider, onTap: () {}),
                  const SizedBox(height: 16),
                  PageSection(
                    title: '预约时段',
                    description: '只展示服务端确认还有名额的时段。',
                    child: widget.provider.availableSlots.isEmpty
                        ? const CompanionEmptyState(
                            title: '暂无可预约时段',
                            description: '可以稍后再看，或选择其他服务商。',
                            icon: Icons.event_busy_outlined,
                          )
                        : DropdownButtonFormField<String>(
                            value: _selectedSlotId,
                            decoration:
                                const InputDecoration(labelText: '选择时段'),
                            items: widget.provider.availableSlots
                                .map((ProviderScheduleSlotSnapshot slot) =>
                                    DropdownMenuItem<String>(
                                      value: slot.slotId,
                                      child: Text(
                                          '${_formatDate(slot.slotDate)} ${slot.displayText}'),
                                    ))
                                .toList(),
                            onChanged: (String? value) {
                              setState(() {
                                _selectedSlotId = value;
                              });
                            },
                          ),
                  ),
                  const SizedBox(height: 16),
                  PageSection(
                    title: '照护说明',
                    description: '把需要服务方提前知道的信息写清楚。',
                    child: Column(
                      children: [
                        TextFormField(
                          controller: _demandController,
                          minLines: 3,
                          maxLines: 5,
                          decoration: const InputDecoration(
                            labelText: '需求说明',
                            hintText: '例如近期症状、洗护注意事项、寄养喂食要求',
                          ),
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _contactNameController,
                          decoration: const InputDecoration(labelText: '联系人'),
                          validator: _requiredValidator,
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          controller: _contactMobileController,
                          keyboardType: TextInputType.phone,
                          decoration: const InputDecoration(labelText: '联系电话'),
                          validator: _requiredValidator,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),
                  FilledButton(
                    onPressed: _submitting ? null : _submit,
                    child: Text(_submitting ? '提交中...' : '提交预约'),
                  ),
                ],
              ),
            ),
    );
  }
}

class ServiceAppointmentListPage extends StatefulWidget {
  const ServiceAppointmentListPage({super.key});

  @override
  State<ServiceAppointmentListPage> createState() =>
      _ServiceAppointmentListPageState();
}

class _ServiceAppointmentListPageState
    extends State<ServiceAppointmentListPage> {
  Future<List<ServiceAppointmentSnapshot>>? _appointmentsFuture;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _appointmentsFuture ??=
        PetLifeAppScope.repositoryOf(context).listServiceAppointments();
  }

  void _reload() {
    setState(() {
      _appointmentsFuture =
          PetLifeAppScope.repositoryOf(context).listServiceAppointments();
    });
  }

  Future<void> _cancel(ServiceAppointmentSnapshot appointment) async {
    final bool confirmed = await showCompanionConfirmSheet(
      context,
      title: '取消这次预约吗',
      description:
          '${appointment.providerName} · ${_formatDate(appointment.appointmentDate)} '
          '${appointment.appointmentSlot}',
      confirmLabel: '确认取消',
      cancelLabel: '先保留',
      confirmColor: AppThemePalette.danger,
    );
    if (!mounted || !confirmed) {
      return;
    }
    try {
      await PetLifeAppScope.repositoryOf(context).cancelServiceAppointment(
        appointmentId: appointment.appointmentId,
        cancelReason: '用户主动取消',
      );
      if (mounted) {
        showCompanionSuccessFeedback(context, '预约已取消');
        _reload();
      }
    } catch (error) {
      if (mounted) {
        showCompanionErrorFeedback(context, error.toString());
      }
    }
  }

  Future<void> _review(ServiceAppointmentSnapshot appointment) async {
    final bool? changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => ServiceReviewEditorPage(appointment: appointment),
      ),
    );
    if (mounted && changed == true) {
      _reload();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('预约记录')),
      body: FutureBuilder<List<ServiceAppointmentSnapshot>>(
        future: _appointmentsFuture,
        builder: (BuildContext context,
            AsyncSnapshot<List<ServiceAppointmentSnapshot>> snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError || !snapshot.hasData) {
            return _ErrorState(
                message: snapshot.error.toString(), onRetry: _reload);
          }
          final List<ServiceAppointmentSnapshot> appointments = snapshot.data!;
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              PageSection(
                title: '全部预约',
                description: '医院、洗护、寄养和训练预约都统一沉淀在这里。',
                child: appointments.isEmpty
                    ? const CompanionEmptyState(
                        title: '还没有预约记录',
                        description: '从服务商详情页提交预约后，会在这里看到进度。',
                        icon: Icons.event_note_outlined,
                      )
                    : Column(
                        children: appointments
                            .map((ServiceAppointmentSnapshot appointment) =>
                                Padding(
                                  padding: const EdgeInsets.only(bottom: 12),
                                  child: _AppointmentCard(
                                    appointment: appointment,
                                    onCancel: appointment.status ==
                                                'pending_confirm' ||
                                            appointment.status == 'confirmed'
                                        ? () => _cancel(appointment)
                                        : null,
                                    onReview:
                                        appointment.status == 'completed' &&
                                                !appointment.reviewed
                                            ? () => _review(appointment)
                                            : null,
                                  ),
                                ))
                            .toList(),
                      ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class ServiceReviewEditorPage extends StatefulWidget {
  const ServiceReviewEditorPage({
    super.key,
    required this.appointment,
  });

  final ServiceAppointmentSnapshot appointment;

  @override
  State<ServiceReviewEditorPage> createState() =>
      _ServiceReviewEditorPageState();
}

class _ServiceReviewEditorPageState extends State<ServiceReviewEditorPage> {
  final TextEditingController _contentController = TextEditingController();
  int _rating = 5;
  bool _submitting = false;

  @override
  void dispose() {
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_submitting) {
      return;
    }
    setState(() {
      _submitting = true;
    });
    try {
      await PetLifeAppScope.repositoryOf(context).createProviderReview(
        appointmentId: widget.appointment.appointmentId,
        draft: ServiceReviewDraft(
          rating: _rating,
          content: _normalizeNullableText(_contentController.text),
        ),
      );
      if (!mounted) {
        return;
      }
      showCompanionSuccessFeedback(context, '评价已提交');
      Navigator.of(context).pop(true);
    } catch (error) {
      if (mounted) {
        showCompanionErrorFeedback(context, error.toString());
      }
    } finally {
      if (mounted) {
        setState(() {
          _submitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('评价服务')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _AppointmentCard(appointment: widget.appointment),
          const SizedBox(height: 16),
          PageSection(
            title: '这次服务感觉如何',
            description: '评价会展示在服务商详情页，也会用于回算服务商评分。',
            child: CompanionCard(
              color: AppThemePalette.surfaceRaised,
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('评分', style: Theme.of(context).textTheme.titleSmall),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8,
                    children: List<Widget>.generate(5, (int index) {
                      final int ratingValue = index + 1;
                      final bool selected = ratingValue <= _rating;
                      return IconButton.filledTonal(
                        onPressed: () {
                          setState(() {
                            _rating = ratingValue;
                          });
                        },
                        icon: Icon(
                          selected
                              ? Icons.star_rounded
                              : Icons.star_outline_rounded,
                          color: selected
                              ? AppThemePalette.primaryDeep
                              : AppThemePalette.muted,
                        ),
                      );
                    }),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _contentController,
                    minLines: 4,
                    maxLines: 6,
                    decoration: const InputDecoration(
                      labelText: '评价内容',
                      hintText: '例如服务是否细心、沟通是否顺畅、宠物回来后的状态',
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            child: Text(_submitting ? '提交中...' : '提交评价'),
          ),
        ],
      ),
    );
  }
}

class _ProviderDetailBody extends StatelessWidget {
  const _ProviderDetailBody({
    required this.provider,
    required this.reviewsFuture,
    required this.onAppointmentTap,
  });

  final ServiceProviderSnapshot provider;
  final Future<List<ProviderReviewSnapshot>> reviewsFuture;
  final VoidCallback onAppointmentTap;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _ProviderCard(provider: provider, onTap: () {}),
        const SizedBox(height: 16),
        PageSection(
          title: '服务项目',
          description: '价格仅作为服务方登记范围，实际履约仍以线下确认为准。',
          child: provider.serviceItems.isEmpty
              ? const CompanionEmptyState(
                  title: '暂未配置服务项目',
                  description: '可以先通过电话确认具体服务内容。',
                  icon: Icons.inventory_2_outlined,
                )
              : Column(
                  children: provider.serviceItems
                      .map((ProviderServiceItemSnapshot item) =>
                          _ServiceItemCard(item: item))
                      .toList(),
                ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '可预约时段',
          description: '时段名额由服务端校验，提交成功后会进入预约记录。',
          child: provider.availableSlots.isEmpty
              ? const CompanionEmptyState(
                  title: '最近暂无可预约时段',
                  description: '可以稍后再看，或选择其他同类服务商。',
                  icon: Icons.event_busy_outlined,
                )
              : Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: provider.availableSlots
                      .map((ProviderScheduleSlotSnapshot slot) => CompanionPill(
                            label:
                                '${_formatDate(slot.slotDate)} ${slot.displayText}',
                            backgroundColor: AppThemePalette.surface,
                          ))
                      .toList(),
                ),
        ),
        const SizedBox(height: 16),
        PageSection(
          title: '服务评价',
          description: '只展示已经完成预约后写下的真实评价。',
          child: FutureBuilder<List<ProviderReviewSnapshot>>(
            future: reviewsFuture,
            builder: (
              BuildContext context,
              AsyncSnapshot<List<ProviderReviewSnapshot>> snapshot,
            ) {
              if (snapshot.connectionState != ConnectionState.done) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snapshot.hasError || !snapshot.hasData) {
                return CompanionEmptyState(
                  title: '评价暂时加载失败',
                  description: snapshot.error.toString(),
                  icon: Icons.rate_review_outlined,
                );
              }
              final List<ProviderReviewSnapshot> reviews = snapshot.data!;
              if (reviews.isEmpty) {
                return const CompanionEmptyState(
                  title: '还没有服务评价',
                  description: '完成预约后的评价会沉淀在这里，帮助其他家长判断服务是否合适。',
                  icon: Icons.rate_review_outlined,
                );
              }
              return Column(
                children: reviews
                    .map((ProviderReviewSnapshot review) =>
                        _ProviderReviewCard(review: review))
                    .toList(),
              );
            },
          ),
        ),
        const SizedBox(height: 20),
        FilledButton.icon(
          onPressed: provider.availableSlots.isEmpty ? null : onAppointmentTap,
          icon: const Icon(Icons.event_available_rounded),
          label: const Text('预约这家服务'),
        ),
      ],
    );
  }
}

class _ServiceHeroSection extends StatelessWidget {
  const _ServiceHeroSection({
    required this.cityName,
    required this.opened,
    required this.onAppointmentsTap,
  });

  final String cityName;
  final bool opened;
  final VoidCallback onAppointmentsTap;

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
        children: [
          CompanionPill(
            label: opened ? '$cityName 已接入服务' : '$cityName 待开通',
            icon: Icons.medical_services_outlined,
            backgroundColor: const Color(0xFFFFE0CF),
            foregroundColor: AppThemePalette.primaryDeep,
          ),
          const SizedBox(height: 12),
          Text('把照护预约整理在一个地方',
              style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 10),
          Text(
            '医院、洗护、寄养和训练都围绕当前宠物发起预约，成功后会沉淀到消息和时间轴里。',
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: AppThemePalette.body,
                ),
          ),
          const SizedBox(height: 16),
          FilledButton.tonalIcon(
            onPressed: onAppointmentsTap,
            icon: const Icon(Icons.receipt_long_outlined),
            label: const Text('预约记录'),
          ),
        ],
      ),
    );
  }
}

class _CategorySection extends StatelessWidget {
  const _CategorySection({
    required this.categories,
    required this.onTap,
  });

  final List<ServiceCategorySnapshot> categories;
  final ValueChanged<ServiceCategorySnapshot> onTap;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: categories
          .map((ServiceCategorySnapshot category) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: CompanionCard(
                  color: AppThemePalette.surfaceRaised,
                  padding: const EdgeInsets.all(16),
                  child: InkWell(
                    onTap: category.available ? () => onTap(category) : null,
                    child: Row(
                      children: [
                        Icon(_categoryIcon(category.providerType),
                            color: AppThemePalette.primaryDeep),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(category.title,
                                  style:
                                      Theme.of(context).textTheme.titleMedium),
                              const SizedBox(height: 4),
                              Text(
                                '${category.description} ${category.providerCount} 家可选',
                                style: Theme.of(context).textTheme.bodySmall,
                              ),
                            ],
                          ),
                        ),
                        Icon(
                          category.available
                              ? Icons.chevron_right_rounded
                              : Icons.lock_clock_outlined,
                          color: AppThemePalette.muted,
                        ),
                      ],
                    ),
                  ),
                ),
              ))
          .toList(),
    );
  }
}

class _ProviderList extends StatelessWidget {
  const _ProviderList({
    required this.providers,
    required this.onTap,
  });

  final List<ServiceProviderSnapshot> providers;
  final ValueChanged<ServiceProviderSnapshot> onTap;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: providers
          .map((ServiceProviderSnapshot provider) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _ProviderCard(
                    provider: provider, onTap: () => onTap(provider)),
              ))
          .toList(),
    );
  }
}

class _ProviderCard extends StatelessWidget {
  const _ProviderCard({
    required this.provider,
    required this.onTap,
  });

  final ServiceProviderSnapshot provider;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      color: AppThemePalette.surfaceRaised,
      padding: const EdgeInsets.all(16),
      child: InkWell(
        onTap: onTap,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(provider.providerName,
                      style: Theme.of(context).textTheme.titleMedium),
                ),
                CompanionPill(
                  label: provider.bookable ? '可预约' : '暂不可约',
                  backgroundColor: provider.bookable
                      ? AppThemePalette.warmTint
                      : AppThemePalette.surface,
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              provider.address ?? '暂未填写地址',
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: AppThemePalette.body),
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                CompanionPill(label: _providerTypeLabel(provider.providerType)),
                if (provider.ratingAvg != null)
                  CompanionPill(label: '${provider.ratingAvg} 分'),
                CompanionPill(label: '${provider.reviewCount} 条评价'),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ServiceItemCard extends StatelessWidget {
  const _ServiceItemCard({required this.item});

  final ProviderServiceItemSnapshot item;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: CompanionCard(
        color: AppThemePalette.surfaceRaised,
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(item.serviceName,
                      style: Theme.of(context).textTheme.titleSmall),
                  if (item.serviceDesc != null) ...[
                    const SizedBox(height: 4),
                    Text(item.serviceDesc!,
                        style: Theme.of(context).textTheme.bodySmall),
                  ],
                ],
              ),
            ),
            Text(_priceRange(item)),
          ],
        ),
      ),
    );
  }
}

class _ProviderReviewCard extends StatelessWidget {
  const _ProviderReviewCard({required this.review});

  final ProviderReviewSnapshot review;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: CompanionCard(
        color: AppThemePalette.surfaceRaised,
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    review.reviewerNickname,
                    style: Theme.of(context).textTheme.titleSmall,
                  ),
                ),
                CompanionPill(label: '${review.rating} 分'),
              ],
            ),
            if (review.petName != null) ...[
              const SizedBox(height: 6),
              Text(
                '服务宠物：${review.petName}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
            if (review.content != null) ...[
              const SizedBox(height: 8),
              Text(
                review.content!,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: AppThemePalette.body,
                    ),
              ),
            ],
            const SizedBox(height: 8),
            Text(
              _formatDate(review.createdAt),
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: AppThemePalette.muted,
                  ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AppointmentPreviewList extends StatelessWidget {
  const _AppointmentPreviewList({required this.appointments});

  final List<ServiceAppointmentSnapshot> appointments;

  @override
  Widget build(BuildContext context) {
    if (appointments.isEmpty) {
      return const CompanionEmptyState(
        title: '还没有近期预约',
        description: '提交医院、洗护、寄养或训练预约后，会优先显示在这里。',
        icon: Icons.event_note_outlined,
      );
    }
    return Column(
      children: appointments
          .map((ServiceAppointmentSnapshot appointment) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: _AppointmentCard(appointment: appointment),
              ))
          .toList(),
    );
  }
}

class _AppointmentCard extends StatelessWidget {
  const _AppointmentCard({
    required this.appointment,
    this.onCancel,
    this.onReview,
  });

  final ServiceAppointmentSnapshot appointment;
  final VoidCallback? onCancel;
  final VoidCallback? onReview;

  @override
  Widget build(BuildContext context) {
    return CompanionCard(
      color: AppThemePalette.surfaceRaised,
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(appointment.providerName,
                    style: Theme.of(context).textTheme.titleMedium),
              ),
              CompanionPill(label: _appointmentStatusLabel(appointment.status)),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            '${appointment.petName} · ${_providerTypeLabel(appointment.appointmentType)} · '
            '${_formatDate(appointment.appointmentDate)} ${appointment.appointmentSlot}',
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: AppThemePalette.body),
          ),
          if (appointment.demandDesc != null) ...[
            const SizedBox(height: 8),
            Text(appointment.demandDesc!,
                style: Theme.of(context).textTheme.bodySmall),
          ],
          if (appointment.status == 'completed' && appointment.reviewed) ...[
            const SizedBox(height: 10),
            CompanionPill(
              label: '已评价',
              icon: Icons.rate_review_outlined,
              backgroundColor: AppThemePalette.warmTint,
            ),
          ],
          if (onCancel != null || onReview != null) ...[
            const SizedBox(height: 12),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                if (onCancel != null)
                  OutlinedButton(
                    onPressed: onCancel,
                    child: const Text('取消预约'),
                  ),
                if (onReview != null)
                  FilledButton.tonalIcon(
                    onPressed: onReview,
                    icon: const Icon(Icons.rate_review_outlined),
                    label: const Text('评价服务'),
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({
    required this.message,
    required this.onRetry,
  });

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: CompanionEmptyState(
          title: '服务数据加载失败',
          description: message,
          icon: Icons.wifi_off_rounded,
          actionLabel: '重试',
          onAction: onRetry,
        ),
      ),
    );
  }
}

class _ServiceHomeViewData {
  const _ServiceHomeViewData({
    required this.currentUser,
    required this.serviceHome,
  });

  final CurrentUserSnapshot currentUser;
  final ServiceHomeSnapshot serviceHome;
}

String? _requiredValidator(String? value) {
  return value == null || value.trim().isEmpty ? '请填写必填信息' : null;
}

String? _normalizeNullableText(String? value) {
  if (value == null) {
    return null;
  }
  final String trimmedValue = value.trim();
  return trimmedValue.isEmpty ? null : trimmedValue;
}

String _formatDate(DateTime date) {
  final String month = date.month.toString().padLeft(2, '0');
  final String day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}

String _providerTypeLabel(String providerType) {
  switch (providerType) {
    case 'hospital':
      return '宠物医院';
    case 'boarding':
      return '寄养照看';
    case 'grooming':
      return '洗护美容';
    case 'training':
      return '训练服务';
    default:
      return '服务';
  }
}

String _appointmentStatusLabel(String status) {
  switch (status) {
    case 'pending_confirm':
      return '待确认';
    case 'confirmed':
      return '已确认';
    case 'completed':
      return '已完成';
    case 'canceled':
      return '已取消';
    default:
      return status;
  }
}

IconData _categoryIcon(String providerType) {
  switch (providerType) {
    case 'hospital':
      return Icons.local_hospital_outlined;
    case 'boarding':
      return Icons.house_siding_outlined;
    case 'grooming':
      return Icons.bathtub_outlined;
    case 'training':
      return Icons.school_outlined;
    default:
      return Icons.medical_services_outlined;
  }
}

String _priceRange(ProviderServiceItemSnapshot item) {
  if (item.priceMin == null && item.priceMax == null) {
    return '到店确认';
  }
  if (item.priceMin == item.priceMax || item.priceMax == null) {
    return '¥${item.priceMin} 起';
  }
  return '¥${item.priceMin}-¥${item.priceMax}';
}
