import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_ui_auth/firebase_ui_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter/src/widgets/framework.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/tenants/data/tenants_repository.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:rent_tracker/src/routing/app_router.dart';

class TenantsScreen extends StatelessWidget {
  const TenantsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text(
            "Tenants",
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          centerTitle: true,
          actions: const [
            _ProfileMenu(),
          ],
        ),
        floatingActionButton: FloatingActionButton(
          child: const Icon(Icons.add),
          onPressed: () => context.goNamed(AppRoute.createTenant.name),
        ),
        body: Consumer(
          builder: (context, ref, child) {
            final tenantsStream = ref.watch(tenantsStreamProvider);

            return Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: tenantsStream.when(
                data: (tenants) {
                  return ListView.builder(
                    itemCount: tenants.length,
                    itemBuilder: (context, index) {
                      final tenant = tenants[index];
                      return _TenantStatus(tenant: tenant, hasPaid: false);
                    },
                  );
                },
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (error, stack) => Center(child: Text('Error: $error')),
              ),
            );
          },
        ));
  }
}

class _ProfileMenu extends ConsumerWidget {
  const _ProfileMenu({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authRepository = ref.watch(authRepositoryProvider);

    return PopupMenuButton<String>(
      onSelected: (value) {
        if (value == 'logout') {
          // Handle logout
          showDialog(
            context: context,
            builder: (context) => AlertDialog(
              title: const Text('Log Out'),
              content: const Text('Are you sure you want to log out?'),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
                TextButton(
                  onPressed: () {
                    authRepository.signOut();
                    Navigator.pop(context);
                  },
                  child: const Text('Log Out'),
                ),
              ],
            ),
          );
        }
      },
      itemBuilder: (context) => [
        const PopupMenuItem(
          value: 'logout',
          child: Row(
            children: [
              Icon(Icons.logout, color: Colors.black54),
              SizedBox(width: 8),
              Text('Log Out'),
            ],
          ),
        ),
      ],
      icon: const Padding(
        padding: EdgeInsets.symmetric(horizontal: 16),
        child: UserAvatar(size: 36.0),
      ),
    );
  }
}

class _TenantStatus extends StatelessWidget {
  const _TenantStatus({super.key, required this.tenant, required this.hasPaid});
  final Tenant tenant;
  final bool hasPaid;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return ListTile(
      title: Text(
        tenant.name,
        style: textTheme.headlineSmall,
      ),
      subtitle: Text(
        "\$${tenant.amount}",
        style: textTheme.bodyLarge,
      ),
      trailing: IntrinsicWidth(
        child: Row(
          mainAxisSize: MainAxisSize.min, // Shrink the Row to the minimum size
          children: [
            Text("Has paid", style: textTheme.bodyLarge),
            Checkbox.adaptive(
              value: hasPaid,
              onChanged: (_) => {},
            ),
          ],
        ),
      ),
    );
  }
}
