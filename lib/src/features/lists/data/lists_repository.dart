import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/domain/tenant_list.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'lists_repository.g.dart';

class ListsRepository {
  const ListsRepository(this._firestore);
  final FirebaseFirestore _firestore;

  static String listsPath(String uid) => "/users/$uid/lists";
  static String listPath(String uid, String id) => "/users/$uid/lists/$id";

  Future<void> addList({required String uid, required String name}) async =>
      _firestore.collection(listsPath(uid)).add({'name': name});

  Future<void> deleteList({required String uid, required String id}) async =>
      _firestore.doc(listPath(uid, id)).delete();

  Future<void> updateList(
          {required String uid, required TenantList list}) async =>
      _firestore.doc(listPath(uid, list.id)).update(list.toMap());

  Stream<TenantList> watchList({required String uid, required String id}) =>
      _firestore
          .doc(listPath(uid, id))
          .withConverter(
              fromFirestore: (snapshot, _) =>
                  TenantList.fromMap(snapshot.data()!, id),
              toFirestore: (list, _) => list.toMap())
          .snapshots()
          .map((snapshot) => snapshot.data()!);

  Stream<List<TenantList>> watchLists({required String uid}) =>
      queryLists(uid: uid)
          .snapshots()
          .map((snapshot) => snapshot.docs.map((doc) => doc.data()).toList());

  Query<TenantList> queryLists({required String uid}) =>
      _firestore.collection(listsPath(uid)).withConverter(
          fromFirestore: (snapshot, _) =>
              TenantList.fromMap(snapshot.data()!, snapshot.id),
          toFirestore: (list, _) => list.toMap());
}

@Riverpod(keepAlive: true)
ListsRepository listsRepository(Ref ref) {
  return ListsRepository(FirebaseFirestore.instance);
}

@riverpod
Stream<List<TenantList>> listsStream(Ref ref) {
  final user = ref.watch(firebaseAuthProvider).currentUser!;
  final repository = ref.watch(listsRepositoryProvider);
  return repository.watchLists(uid: user.uid);
}

@riverpod
Stream<TenantList> listStream(Ref ref, String id) {
  final user = ref.watch(firebaseAuthProvider).currentUser!;
  final repository = ref.watch(listsRepositoryProvider);
  return repository.watchList(uid: user.uid, id: id);
}
