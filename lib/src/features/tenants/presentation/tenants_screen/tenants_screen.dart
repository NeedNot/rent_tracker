import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_ui_auth/firebase_ui_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter/src/widgets/framework.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/tenants/data/tenants_repository.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:rent_tracker/src/features/tenants/presentation/tenants_screen/tenants_screen_controller.dart';
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
                  tenants.sort((tenant1, tenant2) =>
                      tenant1.createdAt.compareTo(tenant2.createdAt));
                  final oldestDate = tenants.first.createdAt;
                  final monthsSince =
                      DateUtils.monthDelta(oldestDate, DateTime.now()) + 1;
                  return ListView.builder(
                    padding: const EdgeInsets.only(bottom: 64),
                    itemCount: monthsSince,
                    itemBuilder: (context, index) {
                      final month =
                          DateUtils.addMonthsToMonthDate(oldestDate, index);
                      final filteredTenants = tenants.where((tenant) {
                        final tenantDate = tenant.createdAt;
                        return tenantDate.difference(month).isNegative ||
                            DateUtils.isSameMonth(month, tenantDate);
                      }).toList();
                      return _MonthStatus(
                        month: month,
                        tenants: filteredTenants,
                      );
                    },
                  );
                },
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (error, stack) {
                  debugPrint('Error: $error');
                  debugPrint('Stack: $stack');
                  return Center(child: Text(error.toString()));
                },
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
  const _TenantStatus(
      {super.key,
      required this.tenant,
      required this.hasPaid,
      required this.onSetPayment,
      this.onTap});
  final Tenant tenant;
  final bool hasPaid;
  final void Function(bool) onSetPayment;
  final VoidCallback? onTap;

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
              onChanged: (value) => {
                if (value != null) {onSetPayment(value)}
              },
            ),
          ],
        ),
      ),
      onTap: onTap,
    );
  }
}

class _MonthStatus extends ConsumerWidget {
  const _MonthStatus({super.key, required this.month, required this.tenants});
  final DateTime month;
  final List<Tenant> tenants;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isCurrentMonth = DateUtils.isSameMonth(month, DateTime.now());
    final filteredTenants = tenants
        .where((tenant) =>
            isCurrentMonth ||
            tenant.payments[DateFormat('yyyy-MM').format(month)] != true)
        .toList();

    if (filteredTenants.isEmpty) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        //month, year
        Text(DateFormat('MMMM yyyy').format(month),
            style: Theme.of(context).textTheme.headlineMedium),
        ListView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemBuilder: (context, index) {
            final tenant = filteredTenants[index];
            final hasPaid =
                tenant.payments[DateFormat('yyyy-MM').format(month)] == true;
            return _TenantStatus(
              tenant: tenant,
              hasPaid: hasPaid,
              onSetPayment: (value) {
                ref
                    .read(tenantsScreenControllerProvider.notifier)
                    .markTenantPayment(
                        id: tenant.id, month: month, paid: value);
              },
              onTap: () => context.goNamed(AppRoute.editTenant.name,
                  pathParameters: {"id": tenant.id}, extra: tenant),
            );
          },
          itemCount: filteredTenants.length,
        ),
      ],
    );
  }
}
