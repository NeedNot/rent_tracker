import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/data/lists_repository.dart';
import 'package:rent_tracker/src/features/tenants/data/tenants_repository.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part "tenants_screen_controller.g.dart";

@riverpod
class ListsScreenController extends _$ListsScreenController {
  @override
  FutureOr<void> build() {}

  Future<void> createDefaultList() async {
    final user = ref.read(firebaseAuthProvider).currentUser!;
    final listsRepository = ref.read(listsRepositoryProvider);

    listsRepository.addList(uid: user.uid, name: "List 1", sharedWith: []);
  }

  Future<bool> makePayment(
      {required String id,
      required String month,
      required int amountPaid}) async {
    state = const AsyncLoading().copyWithPrevious(state);
    final user = ref.read(firebaseAuthProvider).currentUser!;
    state = await AsyncValue.guard(() => ref
        .read(tenantsRepositoryProvider)
        .updatePayment(
            uid: user.uid, id: id, month: month, amountPaid: amountPaid));
    return state.hasError == false;
  }
}
