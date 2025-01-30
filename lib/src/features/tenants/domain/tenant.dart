import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

@immutable
class Tenant extends Equatable {
  const Tenant({
    required this.id,
    required this.listId,
    required this.name,
    required this.amount,
    required this.createdAt,
    required this.payments,
    required this.note,
  });
  final String id;
  final String listId;
  final String name;
  final int amount;
  final String? note;
  final DateTime createdAt;
  final Map<String, int> payments;

  @override
  List<Object?> get props => [listId, name, amount, createdAt];

  @override
  bool get stringify => true;

  factory Tenant.fromMap(Map<String, dynamic> data, String id) {
    final name = data['name'] as String;
    final listId = data['listId'] as String;
    final amount = data['amount'] as int;
    final createdAt =
        ((data['createdAt'] ?? Timestamp.now()) as Timestamp).toDate();
    final payments = (data['payments'] as Map<String, dynamic>? ?? {})
        .map((key, value) => MapEntry(key, value as int));
    final note = data['note'] as String?;
    return Tenant(
        id: id,
        listId: listId,
        name: name,
        amount: amount,
        createdAt: createdAt,
        payments: payments,
        note: note);
  }

  Map<String, dynamic> toMap() {
    return {
      "listId": listId,
      'name': name,
      "amount": amount,
      "note": note,
      "payments": payments
    };
  }
}
