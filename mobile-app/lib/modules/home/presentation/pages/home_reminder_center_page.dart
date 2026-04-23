import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/modules/reminder/presentation/pages/reminder_list_page.dart';

/// 首页提醒中心页。
///
/// 该页复用提醒列表真实链路，但从首页进入时使用更贴近“今日照护中心”的文案与结构表达。
class HomeReminderCenterPage extends StatelessWidget {
  const HomeReminderCenterPage({
    super.key,
    required this.petId,
    required this.petName,
  });

  final String petId;
  final String petName;

  @override
  Widget build(BuildContext context) {
    return ReminderListPage(
      petId: petId,
      petName: petName,
      pageTitle: '提醒中心',
      heroLabel: '今日提醒中心',
      heroDescription: '先把今天和最近要记住的时间点收好，照顾时会更从容。',
      sectionTitle: '需要你留心的提醒',
      sectionDescription: '待处理、已完成和已跳过的提醒都会留在这里，回头看时不容易漏掉。',
      emptyTitle: '提醒中心现在很安静',
      emptyDescription: '这段时间还没有新的照护安排，可以顺手补一条新的提醒计划。',
      createButtonLabel: '新增提醒计划',
    );
  }
}
