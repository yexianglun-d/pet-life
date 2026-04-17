import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';

/// 登录页。
class LoginPage extends StatefulWidget {
  const LoginPage({
    super.key,
    required this.onLoginSuccess,
  });

  final VoidCallback onLoginSuccess;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  late final TextEditingController _mobileController;
  late final TextEditingController _codeController;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _mobileController = TextEditingController(text: '13800000000');
    _codeController = TextEditingController(text: '123456');
  }

  @override
  void dispose() {
    _mobileController.dispose();
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isSubmitting) {
      return;
    }

    final String mobile = _mobileController.text.trim();
    final String code = _codeController.text.trim();
    if (mobile.isEmpty || code.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入手机号和验证码')),
      );
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      await repository.loginBySms(mobile: mobile, code: code);
      if (!mounted) {
        return;
      }
      widget.onLoginSuccess();
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString())),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Scaffold(
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Container(
              padding: const EdgeInsets.all(28),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(28),
                border: Border.all(color: const Color(0xFFE2E8F0)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('欢迎回来', style: textTheme.headlineSmall),
                  const SizedBox(height: 10),
                  Text(
                    '当前阶段使用开发验证码打通登录主链路，后续再接真实短信服务。',
                    style: textTheme.bodyMedium,
                  ),
                  const SizedBox(height: 24),
                  TextField(
                    controller: _mobileController,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(
                      labelText: '手机号',
                      hintText: '请输入手机号',
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _codeController,
                    decoration: const InputDecoration(
                      labelText: '验证码',
                      hintText: '请输入验证码',
                    ),
                  ),
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: _isSubmitting ? null : _submit,
                      child: Text(_isSubmitting ? '登录中...' : '进入应用'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
