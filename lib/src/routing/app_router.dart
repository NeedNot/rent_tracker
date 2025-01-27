import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/authentication/presentation/custom_login_screen.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:rent_tracker/src/features/tenants/presentation/edit_tenant_screen/edit_tenant_screen.dart';
import 'package:rent_tracker/src/features/tenants/presentation/tenants_screen/tenants_screen.dart';
import 'package:rent_tracker/src/routing/go_router_refresh_stream.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'app_router.g.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();

enum AppRoute { home, login, createTenant, editTenant }

@riverpod
GoRouter goRouter(Ref ref) {
  final authRepository = ref.watch(authRepositoryProvider);

  return GoRouter(
    initialLocation: "/login",
    navigatorKey: _rootNavigatorKey,
    redirect: (context, state) {
      final path = state.uri.path;
      final isLoggedIn = authRepository.currentUser != null;
      if (!isLoggedIn && !path.startsWith("/login")) {
        return "/login";
      }
      if (isLoggedIn && path.startsWith("/login")) {
        return "/tenants";
      }
      return null;
    },
    refreshListenable: GoRouterRefreshStream(authRepository.authStateChanges()),
    routes: [
      GoRoute(
        name: AppRoute.login.name,
        path: "/login",
        pageBuilder: (context, state) =>
            const NoTransitionPage(child: CustomLoginScreen()),
      ),
      GoRoute(
        name: AppRoute.home.name,
        path: "/tenants",
        pageBuilder: (context, state) =>
            const NoTransitionPage(child: TenantsScreen()),
        routes: [
          GoRoute(
            name: AppRoute.createTenant.name,
            path: "create",
            pageBuilder: (context, state) =>
                const NoTransitionPage(child: EditTenantScreen()),
          ),
          GoRoute(
            path: ":id",
            name: AppRoute.editTenant.name,
            pageBuilder: (context, state) {
              final tenant = state.extra! as Tenant;
              return MaterialPage(
                child: EditTenantScreen(
                  tenant: tenant,
                ),
              );
            },
          )
        ],
      ),
    ],
  );
}
