import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/applications/list_service.dart';
import 'package:rent_tracker/src/features/lists/data/lists_repository.dart';
import 'package:rent_tracker/src/features/lists/domain/tenant_list.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'edit_list_screen_controller.g.dart';

@riverpod
class EditListScrenController extends _$EditListScrenController {
  @override
  FutureOr<void> build() {}

  Future<bool> submit(
      {TenantList? oldList,
      String? id,
      required String name,
      required List<String> sharedWith}) async {
    final currentUser = ref.watch(firebaseAuthProvider).currentUser!;

    state = const AsyncLoading().copyWithPrevious(state);

    final repository = ref.read(listsRepositoryProvider);
    if (oldList != null) {
      state = await AsyncValue.guard(
        () => repository.updateList(
          uid: currentUser.uid,
          list: TenantList(
            id: oldList.id,
            name: name,
            ownerId: currentUser.uid,
            sharedWith: sharedWith,
          ),
        ),
      );
    } else {
      state = await AsyncValue.guard(() =>
          repository.addList(uid: currentUser.uid, name: name, sharedWith: []));
    }
    return state.hasError == false;
  }

  Future<bool> delete(String id) async {
    state = const AsyncLoading().copyWithPrevious(state);
    final service = ref.read(listServiceProvider);

    state = await AsyncValue.guard(() => service.deleteList(id));
    return state.hasError == false;
  }
}
