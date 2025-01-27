import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

@immutable
class Tenant extends Equatable {
  const Tenant(
      {required this.id,
      required this.name,
      required this.amount,
      required this.createdAt,
      required this.payments});
  final String id;
  final String name;
  final int amount;
  final DateTime createdAt;
  final Map<String, int> payments;

  @override
  List<Object?> get props => [name, amount, createdAt];

  @override
  bool get stringify => true;

  factory Tenant.fromMap(Map<String, dynamic> data, String id) {
    final name = data['name'] as String;
    final amount = data['amount'] as int;
    final createdAt =
        ((data['created_at'] ?? Timestamp.now()) as Timestamp).toDate();
    final payments = (data['payments'] as Map<String, dynamic>? ?? {})
        .map((key, value) => MapEntry(key, value as int));
    return Tenant(
        id: id,
        name: name,
        amount: amount,
        createdAt: createdAt,
        payments: payments);
  }

  Map<String, dynamic> toMap() {
    return {'name': name, "amount": amount};
  }
}
