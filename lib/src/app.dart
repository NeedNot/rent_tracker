import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/routing/app_router.dart';
import 'package:rent_tracker/src/routing/app_startup.dart';

class MainApp extends ConsumerWidget {
  const MainApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final goRouter = ref.watch(goRouterProvider);
    return MaterialApp.router(
      routerConfig: goRouter,
      builder: (_, child) {
        return AppStartupWidget(onLoaded: (_) => child!);
      },
    );
  }
}
