import 'dart:ffi';

import 'package:equatable/equatable.dart';
import 'package:flutter/foundation.dart';

@immutable
class Tenant extends Equatable {
  const Tenant({required this.id, required this.name, required this.amount});
  final String id;
  final String name;
  final Float amount;

  @override
  List<Object?> get props => [name, amount];

  @override
  bool get stringify => true;

  factory Tenant.fromMap(Map<String, dynamic> data, String id) {
    final name = data['name'] as String;
    final amount = data['amount'] as Float;
    return Tenant(id: id, name: name, amount: amount);
  }

  Map<String, dynamic> toMap() {
    return {'name': name, "amount": amount};
  }
}
