import 'package:equatable/equatable.dart';
import 'package:flutter/material.dart';

@immutable
class TenantList extends Equatable {
  const TenantList(
      {required this.id, required this.name, required this.tenants});
  final String id;
  final String name;
  final List<String> tenants;

  @override
  List<Object> get props => [name, tenants];

  @override
  bool get stringify => true;

  factory TenantList.fromMap(Map<String, dynamic> data, String id) {
    final name = data['name'] as String;
    final tenants = (data['tenants'] ?? []) as List<dynamic>;
    return TenantList(
      id: id,
      name: name,
      tenants: tenants.map((e) => e as String).toList(),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'tenants': tenants,
    };
  }
}
