import 'package:flutter/material.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/tenants/data/tenants_repository.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'edit_tenant_screen_controller.g.dart';

@riverpod
class EditTenantScreenController extends _$EditTenantScreenController {
  @override
  FutureOr<void> build() {}

  Future<bool> submit(
      {Tenant? oldTenant, required String name, required int amount}) async {
    final currentUser = ref.read(authRepositoryProvider).currentUser!;

    state = const AsyncLoading().copyWithPrevious(state);

    final repository = ref.read(tenantsRepositoryProvider);
    if (oldTenant != null) {
      state = await AsyncValue.guard(
        () => repository.updateTenant(
            uid: currentUser.uid, id: oldTenant.id, name: name, amount: amount),
      );
    } else {
      state = await AsyncValue.guard(
        () => repository.addTenant(
            uid: currentUser.uid, name: name, amount: amount),
      );
    }
    return state.hasError == false;
  }

  Future<bool> delete(String id) async {
    final currentUser = ref.read(authRepositoryProvider).currentUser!;
    final repository = ref.read(tenantsRepositoryProvider);

    state = const AsyncLoading().copyWithPrevious(state);

    state = await AsyncValue.guard(
        () => repository.deleteTenant(uid: currentUser.uid, id: id));
    return state.hasError == false;
  }
}
