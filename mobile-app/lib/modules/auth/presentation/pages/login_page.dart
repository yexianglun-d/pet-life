import 'dart:async';

import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/auth_sms_send_snapshot.dart';

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
  Timer? _countdownTimer;
  int _countdownSeconds = 0;
  bool _isSendingCode = false;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _mobileController = TextEditingController(text: '13800000000');
    _codeController = TextEditingController(text: '123456');
  }

  @override
  void dispose() {
    _countdownTimer?.cancel();
    _mobileController.dispose();
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _sendSmsCode() async {
    if (_isSendingCode || _countdownSeconds > 0) {
      return;
    }

    final String mobile = _mobileController.text.trim();
    if (!_isValidMobile(mobile)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请输入正确的 11 位手机号')),
      );
      return;
    }

    setState(() {
      _isSendingCode = true;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final AuthSmsSendSnapshot result =
          await repository.sendLoginSmsCode(mobile: mobile);
      if (!mounted) {
        return;
      }
      _codeController.text = result.mockedCode;
      _startCountdown(result.resendInSeconds);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('验证码已发送，当前演示验证码 ${result.mockedCode}')),
      );
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
          _isSendingCode = false;
        });
      }
    }
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

  void _startCountdown(int seconds) {
    _countdownTimer?.cancel();
    setState(() {
      _countdownSeconds = seconds;
    });
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (Timer timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      if (_countdownSeconds <= 1) {
        timer.cancel();
        setState(() {
          _countdownSeconds = 0;
        });
        return;
      }
      setState(() {
        _countdownSeconds -= 1;
      });
    });
  }

  bool _isValidMobile(String mobile) {
    return RegExp(r'^1\d{10}$').hasMatch(mobile);
  }

  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;

    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: <Color>[
              Color(0xFFFFF7F0),
              Color(0xFFFFF1E6),
            ],
          ),
        ),
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Container(
                padding: const EdgeInsets.all(30),
                decoration: BoxDecoration(
                  color: AppThemePalette.surface,
                  borderRadius: BorderRadius.circular(32),
                  border: Border.all(color: AppThemePalette.line),
                  boxShadow: AppThemePalette.softShadow,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const _LoginIllustration(),
                    const SizedBox(height: 22),
                    const _LoginEyebrow(),
                    const SizedBox(height: 12),
                    Text('欢迎回到宠物生活管家', style: textTheme.headlineSmall),
                    const SizedBox(height: 12),
                    Text(
                      '把今天想照顾、记录、陪伴的心情，都留在和毛孩子有关的生活里。',
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
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _codeController,
                            decoration: const InputDecoration(
                              labelText: '验证码',
                              hintText: '请输入验证码',
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        SizedBox(
                          width: 124,
                          child: FilledButton.tonal(
                            onPressed: (_isSendingCode || _countdownSeconds > 0)
                                ? null
                                : _sendSmsCode,
                            child: Text(
                              _isSendingCode
                                  ? '发送中...'
                                  : _countdownSeconds > 0
                                      ? '${_countdownSeconds}s'
                                      : '发送验证码',
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppThemePalette.surfaceRaised,
                        borderRadius: BorderRadius.circular(22),
                      ),
                      child: Text(
                        _countdownSeconds > 0
                            ? '验证码已发送，演示环境会自动填入 123456，$_countdownSeconds 秒后可重新发送。'
                            : '当前演示环境可直接发送验证码并自动填入 123456，真实短信能力会在完整交付阶段接入。',
                        style: textTheme.bodySmall?.copyWith(
                          color: AppThemePalette.body,
                        ),
                      ),
                    ),
                    const SizedBox(height: 24),
                    SizedBox(
                      width: double.infinity,
                      child: FilledButton(
                        onPressed: _isSubmitting ? null : _submit,
                        child: Text(_isSubmitting ? '正在回到首页...' : '进入毛孩子的生活空间'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _LoginEyebrow extends StatelessWidget {
  const _LoginEyebrow();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: AppThemePalette.warmTint,
        borderRadius: BorderRadius.circular(999),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.pets, size: 14, color: AppThemePalette.primaryDeep),
          SizedBox(width: 6),
          Text('陪伴，从今天继续'),
        ],
      ),
    );
  }
}

class _LoginIllustration extends StatelessWidget {
  const _LoginIllustration();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 132,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: <Color>[
            Color(0xFFFFE5D5),
            Color(0xFFFFF2E5),
          ],
        ),
        borderRadius: BorderRadius.circular(28),
      ),
      child: Stack(
        children: [
          Positioned(
            top: 18,
            left: 18,
            child: Container(
              width: 54,
              height: 54,
              decoration: const BoxDecoration(
                color: Color(0x40FFFFFF),
                shape: BoxShape.circle,
              ),
            ),
          ),
          Positioned(
            right: 22,
            top: 18,
            child: Container(
              width: 66,
              height: 66,
              decoration: const BoxDecoration(
                color: Color(0x30FFFFFF),
                shape: BoxShape.circle,
              ),
            ),
          ),
          const Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.pets,
                  size: 42,
                  color: AppThemePalette.primaryDeep,
                ),
                SizedBox(height: 10),
                Text('记录、照护、陪伴'),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
