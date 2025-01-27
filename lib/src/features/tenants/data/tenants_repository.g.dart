// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'tenants_repository.dart';

// **************************************************************************
// RiverpodGenerator
// **************************************************************************

String _$tenantsRepositoryHash() => r'4b0d2bc7ca753c39a517e90fda19a54cd2a6eec5';

/// See also [tenantsRepository].
@ProviderFor(tenantsRepository)
final tenantsRepositoryProvider = Provider<TenantsRepository>.internal(
  tenantsRepository,
  name: r'tenantsRepositoryProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$tenantsRepositoryHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef TenantsRepositoryRef = ProviderRef<TenantsRepository>;
String _$tenantsStreamHash() => r'6497dbf08d4d1ab4e5169d43a753c4aa9933f576';

/// See also [tenantsStream].
@ProviderFor(tenantsStream)
final tenantsStreamProvider = AutoDisposeStreamProvider<List<Tenant>>.internal(
  tenantsStream,
  name: r'tenantsStreamProvider',
  debugGetCreateSourceHash: const bool.fromEnvironment('dart.vm.product')
      ? null
      : _$tenantsStreamHash,
  dependencies: null,
  allTransitiveDependencies: null,
);

@Deprecated('Will be removed in 3.0. Use Ref instead')
// ignore: unused_element
typedef TenantsStreamRef = AutoDisposeStreamProviderRef<List<Tenant>>;
String _$tenantStreamHash() => r'e4e4b05f5bbf911a8a07a991872e7bcee2029401';

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

/// See also [tenantStream].
@ProviderFor(tenantStream)
const tenantStreamProvider = TenantStreamFamily();

/// See also [tenantStream].
class TenantStreamFamily extends Family<AsyncValue<Tenant>> {
  /// See also [tenantStream].
  const TenantStreamFamily();

  /// See also [tenantStream].
  TenantStreamProvider call(
    String id,
  ) {
    return TenantStreamProvider(
      id,
    );
  }

  @override
  TenantStreamProvider getProviderOverride(
    covariant TenantStreamProvider provider,
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
  String? get name => r'tenantStreamProvider';
}

/// See also [tenantStream].
class TenantStreamProvider extends AutoDisposeStreamProvider<Tenant> {
  /// See also [tenantStream].
  TenantStreamProvider(
    String id,
  ) : this._internal(
          (ref) => tenantStream(
            ref as TenantStreamRef,
            id,
          ),
          from: tenantStreamProvider,
          name: r'tenantStreamProvider',
          debugGetCreateSourceHash:
              const bool.fromEnvironment('dart.vm.product')
                  ? null
                  : _$tenantStreamHash,
          dependencies: TenantStreamFamily._dependencies,
          allTransitiveDependencies:
              TenantStreamFamily._allTransitiveDependencies,
          id: id,
        );

  TenantStreamProvider._internal(
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
    Stream<Tenant> Function(TenantStreamRef provider) create,
  ) {
    return ProviderOverride(
      origin: this,
      override: TenantStreamProvider._internal(
        (ref) => create(ref as TenantStreamRef),
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
  AutoDisposeStreamProviderElement<Tenant> createElement() {
    return _TenantStreamProviderElement(this);
  }

  @override
  bool operator ==(Object other) {
    return other is TenantStreamProvider && other.id == id;
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
mixin TenantStreamRef on AutoDisposeStreamProviderRef<Tenant> {
  /// The parameter `id` of this provider.
  String get id;
}

class _TenantStreamProviderElement
    extends AutoDisposeStreamProviderElement<Tenant> with TenantStreamRef {
  _TenantStreamProviderElement(super.provider);

  @override
  String get id => (origin as TenantStreamProvider).id;
}
// ignore_for_file: type=lint
// ignore_for_file: subtype_of_sealed_class, invalid_use_of_internal_member, invalid_use_of_visible_for_testing_member, deprecated_member_use_from_same_package
