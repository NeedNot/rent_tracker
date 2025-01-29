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
String _$listsStreamHash() => r'8fac0dbbe305b94fa52ba66b2e87e3bd99401f33';

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
String _$listStreamHash() => r'62ced8198f517b444a2d8eaba3c2c6de1c75ca20';

/// Copied from Dart SDK
class _SystemHash {
  _SystemHash._();

  static int combine(int hash, int value) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + value);
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x0007ffff & hash) << 10));
    return hash ^ (hash >> 6);
  }

  static int finish(int hash) {
    // ignore: parameter_assignments
    hash = 0x1fffffff & (hash + ((0x03ffffff & hash) << 3));
    // ignore: parameter_assignments
    hash = hash ^ (hash >> 11);
    return 0x1fffffff & (hash + ((0x00003fff & hash) << 15));
  }
}

/// See also [listStream].
@ProviderFor(listStream)
const listStreamProvider = ListStreamFamily();

/// See also [listStream].
class ListStreamFamily extends Family<AsyncValue<TenantList>> {
  /// See also [listStream].
  const ListStreamFamily();

  /// See also [listStream].
  ListStreamProvider call(
    String id,
  ) {
    return ListStreamProvider(
      id,
    );
  }

  @override
  ListStreamProvider getProviderOverride(
    covariant ListStreamProvider provider,
  ) {
    return call(
      provider.id,
    );
  }

  static const Iterable<ProviderOrFamily>? _dependencies = null;

  @override
  Iterable<ProviderOrFamily>? get dependencies => _dependencies;

  static const Iterable<ProviderOrFamily>? _allTransitiveDependencies = null;

  @override
  Iterable<ProviderOrFamily>? get allTransitiveDependencies =>
      _allTransitiveDependencies;

  @override
  String? get name => r'listStreamProvider';
}

/// See also [listStream].
class ListStreamProvider extends AutoDisposeStreamProvider<TenantList> {
  /// See also [listStream].
  ListStreamProvider(
    String id,
  ) : this._internal(
          (ref) => listStream(
            ref as ListStreamRef,
            id,
          ),
          from: listStreamProvider,
          name: r'listStreamProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$listStreamHash,
          dependencies: ListStreamFamily._dependencies,
          allTransitiveDependencies:
              ListStreamFamily._allTransitiveDependencies,
          id: id,
        );

  ListStreamProvider._internal(
    super._createNotifier, {
    required super.name,
    required super.dependencies,
    required super.allTransitiveDependencies,
    required super.debugGetCreateSourceHash,
    required super.from,
    required this.id,
  }) : super.internal();

  final String id;

  @override
  Override overrideWith(
    Stream<TenantList> Function(ListStreamRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: ListStreamProvider._internal(
        (ref) => create(ref as ListStreamRef),
        from: from,
        name: null,
        dependencies: null,
        allTransitiveDependencies: null,
        debugGetCreateSourceHash: null,
        id: id,
      ),
    );
  }

  @override
  AutoDisposeStreamProviderElement<TenantList> createElement() {
    return _ListStreamProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is ListStreamProvider && other.id == id;
  }

  @override
  int get hashCode {
    var hash = _SystemHash.combine(0, runtimeType.hashCode);
    hash = _SystemHash.combine(hash, id.hashCode);

    return _SystemHash.finish(hash);
  }
}

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
mixin ListStreamRef on AutoDisposeStreamProviderRef<TenantList> {
  /// The parameter `id` of this provider.
  String get id;
}

class _ListStreamProviderElement
    extends AutoDisposeStreamProviderElement<TenantList> with ListStreamRef {
  _ListStreamProviderElement(super.provider);

  @override
  String get id => (origin as ListStreamProvider).id;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
