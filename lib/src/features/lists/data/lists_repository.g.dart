// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'lists_repository.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$listsRepositoryHash() => r'2ce239b513ac7bfe90133f873b6973f9360456ae';

/// See also [listsRepository].
@ProviderFor(listsRepository)
final listsRepositoryProvider = Provider<ListsRepository>.internal(
  listsRepository,
  name: r'listsRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$listsRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef ListsRepositoryRef = ProviderRef<ListsRepository>;
String _$listsStreamHash() => r'ec6ae904ac26db1fd071509228ca02613fe4ea09';

/// See also [listsStream].
@ProviderFor(listsStream)
final listsStreamProvider =
    AutoDisposeStreamProvider<List<TenantList>>.internal(
  listsStream,
  name: r'listsStreamProvider',
  debugGetCreateSourceHash:
      const bool.fromEnvironment('dart.vm.product') ? null : _$listsStreamHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef ListsStreamRef = AutoDisposeStreamProviderRef<List<TenantList>>;
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
