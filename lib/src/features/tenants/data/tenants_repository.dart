import 'dart:async';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';
import 'package:rxdart/rxdart.dart';
import 'package:async/async.dart';

part 'tenants_repository.g.dart';

class TenantsRepository {
  const TenantsRepository(this._firestore);
  final FirebaseFirestore _firestore;

  static String tenantPath(String id) => "tenants/$id";
  static String tenantsPath() => "tenants";

  Future<void> addTenant(
          {required String uid,
          required String name,
          required int amount,
          required String? note}) async =>
      _firestore.collection(tenantsPath()).add({
        'name': name,
        'amount': amount,
        'created_at': FieldValue.serverTimestamp(),
        'note': note
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

  Future<void> markTenantPayment(
      {required String uid,
      required String id,
      required String month,
      required int amountPaid}) async {
    _firestore.doc(tenantPath(id)).update({'payments.$month': amountPaid});
  }

  Future<void> deleteTenant({required String uid, required String id}) async {
    // todo delete payments associated with tenant
    _firestore.doc(tenantPath(id)).delete();
  }

  Stream<Tenant> watchTenant({required String id}) => _firestore
      .doc(tenantPath(id))
      .withConverter(
          fromFirestore: (snapshot, _) => Tenant.fromMap(snapshot.data()!, id),
          toFirestore: (tenant, _) => tenant.toMap())
      .snapshots()
      .map((snapshot) => snapshot.data()!);

  Stream<List<Tenant>> watchTenants({required List<String> ids}) {
    // Create a stream for each tenant ID
    List<Stream<Tenant>> tenantStreams = ids.map((id) {
      return watchTenant(id: id);
    }).toList();

    // Combine the streams using Rx.combineLatestList
    return Rx.combineLatestList(tenantStreams);
  }

  Query<Tenant> queryTenants() =>
      _firestore.collection(tenantsPath()).withConverter<Tenant>(
            fromFirestore: (snapshot, _) =>
                Tenant.fromMap(snapshot.data()!, snapshot.id),
            toFirestore: (tenant, _) => tenant.toMap(),
          );
}

@Riverpod(keepAlive: true)
TenantsRepository tenantsRepository(Ref ref) {
  return TenantsRepository(FirebaseFirestore.instance);
}

@riverpod
Stream<Tenant> tenantStream(Ref ref, String id) {
  final repository = ref.watch(tenantsRepositoryProvider);
  return repository.watchTenant(id: id);
}
