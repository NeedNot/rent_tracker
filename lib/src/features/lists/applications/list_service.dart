import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/data/lists_repository.dart';
import 'package:rent_tracker/src/features/tenants/data/tenants_repository.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'list_service.g.dart';

class ListService {
  const ListService(
      {required this.uid,
      required this.listsRepository,
      required this.tenantsRepository});

  final String uid;
  final ListsRepository listsRepository;
  final TenantsRepository tenantsRepository;

  Future<void> deleteList(String id) async {
    await tenantsRepository.deleteTenants(id);
    await listsRepository.deleteList(uid: uid, id: id);
  }
}

@riverpod
ListService listService(Ref ref) {
  final user = ref.watch(firebaseAuthProvider).currentUser!;
  return ListService(
      uid: user.uid,
      listsRepository: ref.watch(listsRepositoryProvider),
      tenantsRepository: ref.watch(tenantsRepositoryProvider));
}
