import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/data/lists_repository.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'edit_list_screen_controller.g.dart';

@riverpod
class EditListScrenController extends _$EditListScrenController {
  @override
  FutureOr<void> build() {}

  Future<bool> submit(
      {String? id,
      required String name,
      required List<String> sharedWith}) async {
    final currentUser = ref.watch(firebaseAuthProvider).currentUser!;

    state = const AsyncLoading().copyWithPrevious(state);
    state = await AsyncValue.guard(() => ref
        .read(listsRepositoryProvider)
        .addList(uid: currentUser.uid, name: name, sharedWith: sharedWith));
    return state.hasError == false;
  }
}
