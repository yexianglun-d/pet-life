import 'dart:async';

import 'package:flutter/material.dart';
import 'package:petlife_mobile_app/app/theme/app_theme.dart';
import 'package:petlife_mobile_app/modules/common/presentation/widgets/companion_feedback.dart';
import 'package:petlife_mobile_app/shared/app_scope.dart';
import 'package:petlife_mobile_app/shared/domain/models/auth_sms_send_snapshot.dart';
import 'package:petlife_mobile_app/shared/network/api_exception.dart';

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

enum _AuthAction {
  sendCode,
  login,
}

class _LoginPageState extends State<LoginPage> {
  static const String _smsSentMessage = '验证码已发送，请留意短信';

  late final TextEditingController _mobileController;
  late final TextEditingController _codeController;
  Timer? _countdownTimer;
  int _countdownSeconds = 0;
  bool _isSendingCode = false;
  bool _isSubmitting = false;
  bool _hasRequestedCode = false;
  String? _authNoticeMessage;
  CompanionFeedbackTone _authNoticeTone = CompanionFeedbackTone.info;

  @override
  void initState() {
    super.initState();
    _mobileController = TextEditingController();
    _codeController = TextEditingController();
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
      _setAuthNotice('请输入正确的 11 位手机号', CompanionFeedbackTone.error);
      showCompanionErrorFeedback(context, '请输入正确的 11 位手机号');
      return;
    }

    setState(() {
      _isSendingCode = true;
      _authNoticeMessage = null;
    });

    try {
      final repository = PetLifeAppScope.repositoryOf(context);
      final AuthSmsSendSnapshot result =
          await repository.sendLoginSmsCode(mobile: mobile);
      if (!mounted) {
        return;
      }
      if (!result.sent) {
        const String message = '验证码暂时没有发送成功，请稍后重试';
        _setAuthNotice(message, CompanionFeedbackTone.error);
        showCompanionErrorFeedback(context, message);
        return;
      }
      setState(() {
        _hasRequestedCode = true;
        _authNoticeMessage = null;
      });
      _startCountdown(result.resendInSeconds);
      showCompanionSuccessFeedback(context, _smsSentMessage);
    } catch (error) {
      if (!mounted) {
        return;
      }
      final String message = _messageForAuthError(
        error,
        action: _AuthAction.sendCode,
      );
      final CompanionFeedbackTone tone = _feedbackToneForAuthError(error);
      _setAuthNotice(message, tone);
      if (tone == CompanionFeedbackTone.warning) {
        showCompanionFeedback(
          context,
          message: message,
          tone: CompanionFeedbackTone.warning,
        );
      } else {
        showCompanionErrorFeedback(context, message);
      }
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
    if (!_isValidMobile(mobile)) {
      _setAuthNotice('请输入正确的 11 位手机号', CompanionFeedbackTone.error);
      showCompanionErrorFeedback(context, '请输入正确的 11 位手机号');
      return;
    }
    if (!RegExp(r'^\d{6}$').hasMatch(code)) {
      _setAuthNotice('请输入 6 位短信验证码', CompanionFeedbackTone.error);
      showCompanionErrorFeedback(context, '请输入 6 位短信验证码');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _authNoticeMessage = null;
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
      final String message = _messageForAuthError(
        error,
        action: _AuthAction.login,
      );
      final CompanionFeedbackTone tone = _feedbackToneForAuthError(error);
      _setAuthNotice(message, tone);
      if (tone == CompanionFeedbackTone.warning) {
        showCompanionFeedback(
          context,
          message: message,
          tone: CompanionFeedbackTone.warning,
        );
      } else {
        showCompanionErrorFeedback(context, message);
      }
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
    if (seconds <= 0) {
      setState(() {
        _countdownSeconds = 0;
      });
      return;
    }
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

  void _setAuthNotice(String message, CompanionFeedbackTone tone) {
    setState(() {
      _authNoticeMessage = message;
      _authNoticeTone = tone;
    });
  }

  String _currentAuthNoticeMessage() {
    final String? authNoticeMessage = _authNoticeMessage;
    if (authNoticeMessage != null) {
      return authNoticeMessage;
    }
    if (_hasRequestedCode && _countdownSeconds > 0) {
      return '$_smsSentMessage。$_countdownSeconds 秒后可再次发送';
    }
    if (_hasRequestedCode) {
      return '如果还没有收到短信，可以重新获取一条新的验证码';
    }
    return '输入手机号后获取 6 位短信验证码。验证码由服务端生成，App 不展示验证码内容。';
  }

  CompanionFeedbackTone _currentAuthNoticeTone() {
    if (_authNoticeMessage != null) {
      return _authNoticeTone;
    }
    return _hasRequestedCode && _countdownSeconds > 0
        ? CompanionFeedbackTone.success
        : CompanionFeedbackTone.info;
  }

  CompanionFeedbackTone _feedbackToneForAuthError(Object error) {
    if (error is ApiException &&
        (error.responseCode == 'AUTH_SMS_SEND_RATE_LIMITED' ||
            error.responseCode == 'AUTH_SMS_CODE_ATTEMPT_LIMITED')) {
      return CompanionFeedbackTone.warning;
    }
    return CompanionFeedbackTone.error;
  }

  String _messageForAuthError(
    Object error, {
    required _AuthAction action,
  }) {
    if (error is ApiException) {
      switch (error.responseCode) {
        case 'AUTH_SMS_CODE_INVALID':
          return '验证码不正确，请重新输入';
        case 'AUTH_SMS_CODE_EXPIRED':
          return '验证码已过期，请重新获取';
        case 'AUTH_SMS_CODE_USED':
          return '验证码已使用，请重新获取';
        case 'AUTH_SMS_CODE_ATTEMPT_LIMITED':
          return '验证码错误次数过多，请重新获取';
        case 'AUTH_SMS_SEND_RATE_LIMITED':
          if (_countdownSeconds > 0) {
            return '验证码发送太频繁，请 $_countdownSeconds 秒后再试';
          }
          return '验证码发送太频繁，请稍后再试';
        case 'AUTH_SMS_SEND_FAILED':
          return '验证码暂时发送失败，请稍后重试';
      }

      final String serverMessage = error.message.trim();
      if (serverMessage.isNotEmpty) {
        return serverMessage;
      }
    }

    return action == _AuthAction.sendCode
        ? '验证码暂时发送失败，请稍后重试'
        : '登录暂时没有成功，请稍后重试';
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
                            keyboardType: TextInputType.number,
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
                    CompanionFormNotice(
                      message: _currentAuthNoticeMessage(),
                      tone: _currentAuthNoticeTone(),
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
