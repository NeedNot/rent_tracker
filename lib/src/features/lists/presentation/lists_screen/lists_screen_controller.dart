import 'package:flutter/foundation.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/data/lists_repository.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part "lists_screen_controller.g.dart";

@riverpod
class ListsScreenController extends _$ListsScreenController {
  @override
  FutureOr<void> build() {}

  Future<void> createDefaultList() async {
    final user = ref.read(firebaseAuthProvider).currentUser!;
    final listsRepository = ref.read(listsRepositoryProvider);
    
    listsRepository.addList(uid: user.uid, name: "List 1", sharedWith: []);
  }
}
