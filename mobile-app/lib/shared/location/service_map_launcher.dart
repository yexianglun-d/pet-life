import 'package:flutter/foundation.dart';
import 'package:petlife_mobile_app/shared/domain/models/service_center_snapshot.dart';
import 'package:url_launcher/url_launcher.dart';

class ServiceMapLauncher {
  const ServiceMapLauncher();

  Future<bool> openProviderNavigation(ServiceProviderSnapshot provider) async {
    if (!provider.hasCoordinate) {
      return false;
    }

    try {
      final Uri? nativeUri = _buildNativeAmapUri(provider);
      if (nativeUri != null &&
          await launchUrl(nativeUri, mode: LaunchMode.externalApplication)) {
        return true;
      }
    } catch (_) {
      // 高德 App 未安装或系统拒绝自定义 scheme 时，继续尝试网页地图。
    }

    try {
      return launchUrl(
        _buildAmapWebUri(provider),
        mode: LaunchMode.externalApplication,
      );
    } catch (_) {
      return false;
    }
  }

  Uri? _buildNativeAmapUri(ServiceProviderSnapshot provider) {
    final String query = _encodeQuery(<String, String>{
      'sourceApplication': 'petlife',
      'dlat': provider.latitude!.toStringAsFixed(6),
      'dlon': provider.longitude!.toStringAsFixed(6),
      'dname': provider.providerName,
      'dev': '0',
      't': '0',
    });

    return switch (defaultTargetPlatform) {
      TargetPlatform.android => Uri.parse('amapuri://route/plan/?$query'),
      TargetPlatform.iOS => Uri.parse('iosamap://path?$query'),
      _ => null,
    };
  }

  Uri _buildAmapWebUri(ServiceProviderSnapshot provider) {
    return Uri.https('uri.amap.com', '/navigation', <String, String>{
      'to':
          '${provider.longitude!.toStringAsFixed(6)},${provider.latitude!.toStringAsFixed(6)},${provider.providerName}',
      'mode': 'car',
      'policy': '1',
      'src': 'petlife',
      'coordinate': 'gaode',
    });
  }

  String _encodeQuery(Map<String, String> params) {
    return params.entries
        .map((MapEntry<String, String> entry) =>
            '${Uri.encodeComponent(entry.key)}=${Uri.encodeComponent(entry.value)}')
        .join('&');
  }
}
