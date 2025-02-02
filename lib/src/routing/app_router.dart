import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/authentication/presentation/custom_login_screen.dart';
import 'package:rent_tracker/src/features/lists/domain/tenant_list.dart';
import 'package:rent_tracker/src/features/lists/presentation/edit_list_screen_controller/edit_list_screen.dart';
import 'package:rent_tracker/src/features/lists/presentation/list_screen/list_screen.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:rent_tracker/src/features/tenants/presentation/edit_tenant_screen/edit_tenant_screen.dart';
import 'package:rent_tracker/src/features/tenants/presentation/tenants_screen/tenants_screen.dart';
import 'package:rent_tracker/src/routing/go_router_refresh_stream.dart';
import 'package:rent_tracker/src/routing/scaffold_with_nested_navigation.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'app_router.g.dart';

final _rootNavigatorKey = GlobalKey<NavigatorState>();
final _tenantNavigatorKey = GlobalKey<NavigatorState>(debugLabel: "tenants");
final _tenantListNavigatorKey =
    GlobalKey<NavigatorState>(debugLabel: "tenantsList");

enum AppRoute {
  home,
  login,
  lists,
  createList,
  editList,
  createTenant,
  editTenant
}

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
      StatefulShellRoute.indexedStack(
        pageBuilder: (context, state, navigationShell) => NoTransitionPage(
          child: ScaffoldWithNestedNavigation(
            navigationShell: navigationShell,
          ),
        ),
        branches: [
          StatefulShellBranch(
            navigatorKey: _tenantNavigatorKey,
            routes: [
              GoRoute(
                name: AppRoute.home.name,
                path: "/tenants",
                pageBuilder: (context, state) =>
                    const NoTransitionPage(child: TenantsScreen()),
                routes: [
                  GoRoute(
                      name: AppRoute.createTenant.name,
                      path: "/create",
                      pageBuilder: (context, state) =>
                          const NoTransitionPage(child: EditTenantScreen())),
                  GoRoute(
                    name: AppRoute.editTenant.name,
                    path: "/tenants/:id",
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
          ),
          StatefulShellBranch(
            navigatorKey: _tenantListNavigatorKey,
            routes: [
              GoRoute(
                name: AppRoute.lists.name,
                path: "/lists",
                pageBuilder: (context, state) =>
                    const NoTransitionPage(child: ListScreen()),
                routes: [
                  GoRoute(
                    name: AppRoute.createList.name,
                    path: "/create",
                    pageBuilder: (context, state) =>
                        const NoTransitionPage(child: EditListScreen()),
                  ),
                  GoRoute(
                    name: AppRoute.editList.name,
                    path: "/lists/:id",
                    pageBuilder: (context, state) {
                      final list = state.extra! as TenantList;
                      return MaterialPage(
                        child: EditListScreen(
                          list: list,
                        ),
                      );
                    },
                  )
                ],
              )
            ],
          )
        ],
      ),
    ],
  );
}
