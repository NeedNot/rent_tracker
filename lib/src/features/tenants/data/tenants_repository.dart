import 'dart:ffi';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'tenants_repository.g.dart';

class TenantsRepository {
  const TenantsRepository(this._firestore);
  final FirebaseFirestore _firestore;

  static String tenantPath(String uid, String tenantId) =>
      "users/$uid/tenants/$tenantId";
  static String tenantsPath(String uid) => "users/$uid/tenants";

  Future<void> addTenant(
          {required String uid,
          required String name,
          required int amount}) async =>
      _firestore
          .collection(tenantsPath(uid))
          .add({'name': name, 'amount': amount});

  Future<void> updateTenant(
          {required String uid, required Tenant tenant}) async =>
      _firestore.doc(tenantPath(uid, tenant.id)).update(tenant.toMap());

  Future<void> deleteTenant({required String uid, required String id}) async {
    // todo delete payments associated with tenant
    _firestore.doc(tenantPath(uid, id)).delete();
  }

  Stream<Tenant> watchTenant({required String uid, required String id}) =>
      _firestore
          .doc(tenantPath(uid, id))
          .withConverter(
              fromFirestore: (snapshot, _) =>
                  Tenant.fromMap(snapshot.data()!, id),
              toFirestore: (tenant, _) => tenant.toMap())
          .snapshots()
          .map((snapshot) => snapshot.data()!);

  Stream<List<Tenant>> watchTenants({required String uid}) =>
      queryTenants(uid: uid)
          .snapshots()
          .map((snapshot) => snapshot.docs.map((doc) => doc.data()).toList());

  Query<Tenant> queryTenants({required String uid}) =>
      _firestore.collection(tenantsPath(uid)).withConverter(
          fromFirestore: (snapshot, _) =>
              Tenant.fromMap(snapshot.data()!, snapshot.id),
          toFirestore: (tenant, _) => tenant.toMap());
}

@Riverpod(keepAlive: true)
TenantsRepository tenantsRepository(Ref ref) {
  return TenantsRepository(FirebaseFirestore.instance);
}

@Riverpod(keepAlive: true)
Stream<List<Tenant>> tenantsStream(Ref ref) {
  final user = ref.watch(firebaseAuthProvider).currentUser!;
  final repository = ref.watch(tenantsRepositoryProvider);
  return repository.watchTenants(uid: user.uid);
}

@riverpod
Stream<Tenant> tenantStream(Ref ref, String id) {
  final user = ref.watch(firebaseAuthProvider).currentUser!;
  final repository = ref.watch(tenantsRepositoryProvider);
  return repository.watchTenant(uid: user.uid, id: id);
}
