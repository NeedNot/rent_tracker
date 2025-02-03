import 'package:intl/intl.dart';
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
      {Tenant? oldTenant,
      required String name,
      required String listId,
      required int amount,
      required String? note}) async {
    final currentUser = ref.read(authRepositoryProvider).currentUser!;

    state = const AsyncLoading().copyWithPrevious(state);

    final repository = ref.read(tenantsRepositoryProvider);
    if (oldTenant != null) {
      state = await AsyncValue.guard(
        () => repository.updateTenant(
            uid: currentUser.uid,
            id: oldTenant.id,
            name: name,
            amount: amount,
            note: note,
            listId: listId),
      );
    } else {
      state = await AsyncValue.guard(() => repository.addTenant(
          uid: currentUser.uid,
          listId: listId,
          name: name,
          amount: amount,
          note: note));
    }
    return state.hasError == false;
  }

  Future<bool> delete(String id) async {
    final repository = ref.read(tenantsRepositoryProvider);

    state = const AsyncLoading().copyWithPrevious(state);

    state = await AsyncValue.guard(() => repository.deleteTenant(id: id));
    return state.hasError == false;
  }

  Future<bool> updatePayment(
      {required String id,
      required DateTime month,
      required int amountPaid}) async {
    final currentUser = ref.read(firebaseAuthProvider).currentUser!;

    state = const AsyncLoading().copyWithPrevious(state);

    state = await AsyncValue.guard(() async {
      ref.read(tenantsRepositoryProvider).updatePayment(
            uid: currentUser.uid,
            id: id,
            month: DateFormat('yyyy-MM').format(month),
            amountPaid: amountPaid,
          );
    });

    return state.hasError == false;
  }
}
