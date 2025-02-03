import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/domain/tenant_list.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:rxdart/rxdart.dart';

part 'lists_repository.g.dart';

class ListsRepository {
  const ListsRepository(this._firestore);
  final FirebaseFirestore _firestore;

  static String listsPath() => "lists";
  static String listPath(String uid, String id) => "lists/$id";

  Future<void> addList(
          {required String uid,
          required String name,
          required List<String> sharedWith}) async =>
      _firestore
          .collection(listsPath())
          .add({'name': name, 'ownerId': uid, 'sharedWith': sharedWith});

  Future<void> deleteList({required String uid, required String id}) async =>
      _firestore.doc(listPath(uid, id)).delete();

  Future<void> updateList(
          {required String uid, required TenantList list}) async =>
      _firestore.doc(listPath(uid, list.id)).update(list.toMap());

  Stream<List<TenantList>> watchLists(
      {required String uid, required String email, String? id}) {
    final ownedListsQuery = queryLists(uid: uid, id: id)
        .snapshots()
        .map((snapshot) => snapshot.docs.map((doc) => doc.data()).toList());

    final sharedListsQuery = _firestore
        .collection(listsPath())
        .where('sharedWith', arrayContains: email)
        .withConverter(
            fromFirestore: (snapshot, _) =>
                TenantList.fromMap(snapshot.data()!, snapshot.id),
            toFirestore: (TenantList list, _) => list.toMap())
        .snapshots()
        .map((snapshot) => snapshot.docs.map((doc) => doc.data()).toList());

    return Rx.combineLatest2(
      ownedListsQuery,
      sharedListsQuery,
      (owned, shared) => [...owned, ...shared],
    );
  }

  Query<TenantList> queryLists({required String uid, String? id}) {
    Query<TenantList> query = _firestore
        .collection(listsPath())
        .where('ownerId', isEqualTo: uid)
        .withConverter(
            fromFirestore: (snapshot, _) =>
                TenantList.fromMap(snapshot.data()!, snapshot.id),
            toFirestore: (TenantList list, _) => list.toMap());
    if (id != null) {
      query = query.where('id', isEqualTo: id);
    }
    return query;
  }
}

@Riverpod(keepAlive: true)
ListsRepository listsRepository(Ref ref) {
  return ListsRepository(FirebaseFirestore.instance);
}

@riverpod
Stream<List<TenantList>> listsStream(Ref ref) {
  final user = ref.read(firebaseAuthProvider).currentUser!;
  final repository = ref.watch(listsRepositoryProvider);
  return repository.watchLists(uid: user.uid, email: user.email!);
}
