import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

@immutable
class Tenant extends Equatable {
  const Tenant(
      {required this.id,
      required this.name,
      required this.amount,
      required this.createdAt});
  final String id;
  final String name;
  final int amount;
  final DateTime createdAt;

  @override
  List<Object?> get props => [name, amount, createdAt];

  @override
  bool get stringify => true;

  factory Tenant.fromMap(Map<String, dynamic> data, String id) {
    final name = data['name'] as String;
    final amount = data['amount'] as int;
    final createdAt = (data['created_at'] as Timestamp).toDate();
    return Tenant(id: id, name: name, amount: amount, createdAt: createdAt);
  }

  Map<String, dynamic> toMap() {
    return {'name': name, "amount": amount};
  }
}
