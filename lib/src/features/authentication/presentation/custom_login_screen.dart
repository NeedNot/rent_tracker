import 'package:firebase_ui_auth/firebase_ui_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/features/authentication/presentation/auth_providers.dart';

class CustomLoginScreen extends ConsumerWidget {
  const CustomLoginScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authProviders = ref.watch(authProvidersProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Sign in'),
        centerTitle: true,
      ),
      body: SignInScreen(
        providers: authProviders,
        headerBuilder: null,
        showAuthActionSwitch: false,
        footerBuilder: null,
      ),
    );
  }
}
