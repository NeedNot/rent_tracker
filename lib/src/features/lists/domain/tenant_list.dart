import 'package:equatable/equatable.dart';
import 'package:flutter/material.dart';

@immutable
class TenantList extends Equatable {
  const TenantList(
      {required this.id,
      required this.name,
      required this.ownerId,
      required this.sharedWith});
  final String id;
  final String name;
  final String ownerId;
  final List<String> sharedWith;

  @override
  List<Object> get props => [name, ownerId, sharedWith];

  @override
  bool get stringify => true;

  factory TenantList.fromMap(Map<String, dynamic> data, String id) {
    final name = data['name'] as String;
    final ownerId = data['ownerId'] as String;
    final sharedWith = ((data['sharedWith'] ?? []) as List<dynamic>)
        .map((e) => e as String)
        .toList();
    return TenantList(
      id: id,
      name: name,
      ownerId: ownerId,
      sharedWith: sharedWith,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'sharedWith': sharedWith,
    };
  }
}
