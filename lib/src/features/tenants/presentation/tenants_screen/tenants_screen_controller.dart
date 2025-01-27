import 'package:intl/intl.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/tenants/data/tenants_repository.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part "tenants_screen_controller.g.dart";

@riverpod
class TenantsScreenController extends _$TenantsScreenController {
  @override
  FutureOr<void> build() {}

  Future<void> markTenantPayment(
      {required String id, required DateTime month, required int amountPaid}) async {
    final currentUser = ref.watch(firebaseAuthProvider).currentUser!;

    ref.read(tenantsRepositoryProvider).markTenantPayment(
          uid: currentUser.uid,
          id: id,
          month: DateFormat('yyyy-MM').format(month),
          amountPaid: amountPaid,
        );
  }
}
