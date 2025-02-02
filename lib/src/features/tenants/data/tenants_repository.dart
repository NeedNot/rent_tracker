import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

part 'tenants_repository.g.dart';

class TenantsRepository {
  const TenantsRepository(this._firestore);
  final FirebaseFirestore _firestore;

  static String tenantPath(String id) => "tenants/$id";
  static String tenantsPath() => "tenants";

  Future<void> addTenant(
          {required String uid,
          required String listId,
          required String name,
          required int amount,
          required String? note}) async =>
      _firestore.collection(tenantsPath()).add({
        'listId': listId,
        'name': name,
        'amount': amount,
        'note': note,
        'createdAt': FieldValue.serverTimestamp(),
        'payments': {}
      });

  Future<void> updateTenant(
          {required String uid,
          required String id,
          required String name,
          required int amount,
          required String? note}) async =>
      _firestore
          .doc(tenantPath(id))
          .update({'name': name, 'amount': amount, 'note': note});

  Future<void> updatePayment(
      {required String uid,
      required String id,
      required String month,
      required int amountPaid}) async {
    _firestore.doc(tenantPath(id)).update({'payments.$month': amountPaid});
  }

  Future<void> deleteTenant({required String uid, required String id}) async {
    _firestore.doc(tenantPath(id)).delete();
  }

  Stream<Tenant> watchTenant({required String uid, required String id}) {
    return _firestore
        .doc(tenantPath(id))
        .withConverter(
            fromFirestore: (snapshot, _) =>
                Tenant.fromMap(snapshot.data()!, snapshot.id),
            toFirestore: (Tenant tenant, _) => tenant.toMap())
        .snapshots()
        .map((snapshot) => snapshot.data()!);
  }

  Stream<List<Tenant>> watchTenants({required String listId}) =>
      queryTenants(listId: listId)
          .snapshots()
          .map((snapshot) => snapshot.docs.map((doc) => doc.data()).toList());

  Query<Tenant> queryTenants({required String listId}) {
    return _firestore
        .collection(tenantsPath())
        .where('listId', isEqualTo: listId)
        .withConverter(
            fromFirestore: (snapshot, _) =>
                Tenant.fromMap(snapshot.data()!, snapshot.id),
            toFirestore: (Tenant tenant, _) => tenant.toMap());
  }
}

@Riverpod(keepAlive: true)
TenantsRepository tenantsRepository(Ref ref) {
  return TenantsRepository(FirebaseFirestore.instance);
}
