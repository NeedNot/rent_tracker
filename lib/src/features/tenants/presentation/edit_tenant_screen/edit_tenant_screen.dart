import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/data/lists_repository.dart';
import 'package:rent_tracker/src/features/tenants/data/tenants_repository.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:rent_tracker/src/features/tenants/presentation/edit_tenant_screen/edit_tenant_screen_controller.dart';

class EditTenantScreen extends ConsumerStatefulWidget {
  const EditTenantScreen({super.key, this.tenant});
  final Tenant? tenant;

  @override
  ConsumerState<EditTenantScreen> createState() => _CreateTenantScreenState();
}

class _CreateTenantScreenState extends ConsumerState<EditTenantScreen> {
  final _formKey = GlobalKey<FormState>();

  String? _name;
  int? _amount;
  String? _note;
  String? _listId;

  @override
  void initState() {
    super.initState();
    if (widget.tenant != null) {
      _name = widget.tenant?.name;
      _amount = widget.tenant?.amount;
      _note = widget.tenant?.note;
    }
  }

  bool _validateAndSaveForm() {
    final form = _formKey.currentState!;
    if (form.validate() && _listId != null) {
      form.save();
      return true;
    }
    return false;
  }

  Future<void> _submit() async {
    if (_validateAndSaveForm()) {
      final success =
          await ref.read(editTenantScreenControllerProvider.notifier).submit(
                oldTenant: widget.tenant,
                listId: _listId!,
                name: _name ?? '',
                amount: _amount ?? 0,
                note: _note,
              );
      if (success && mounted) {
        // todo the routes have nothing to pop to
        context.pop();
      }
    }
  }

  Future<void> _delete(BuildContext context) async {
    showDialog(
        context: context,
        builder: (context) {
          return AlertDialog(
            title: const Text('Delete Tenant?'),
            content: const Text('Are you sure you want to delete this tenant?'),
            actions: <Widget>[
              TextButton(
                child: const Text('Cancel'),
                onPressed: () => context.pop(),
              ),
              TextButton(
                child: const Text('Delete'),
                onPressed: () async {
                  final success = await ref
                      .read(editTenantScreenControllerProvider.notifier)
                      .delete(widget.tenant!.id);
                  if (success && mounted) {
                    // ignore: use_build_context_synchronously
                    context.pop();
                    // ignore: use_build_context_synchronously
                    context.pop();
                  }
                },
              ),
            ],
          );
        });
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<AsyncValue>(
      editTenantScreenControllerProvider,
      (_, state) {
        if (state.hasError) {
          showDialog(
            context: context,
            builder: (context) => AlertDialog(
              title: const Text('Error'),
              content: Text(state.error?.toString() ?? ''),
              actions: <Widget>[
                TextButton(
                  onPressed: () => context.pop(),
                  child: const Text('OK'),
                ),
              ],
            ),
          );
        }
      },
    );
    final state = ref.watch(editTenantScreenControllerProvider);
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.tenant == null ? 'Create Tenant' : 'Edit Tenant'),
        actions: <Widget>[
          if (widget.tenant != null)
            TextButton(
              onPressed: state.isLoading ? null : () => _delete(context),
              child: const Text('Delete'),
            ),
          TextButton(
            onPressed: state.isLoading ? null : _submit,
            child: const Text('Save'),
          ),
        ],
      ),
      body: Column(children: [
        _buildContents(),
        if (widget.tenant != null)
          Expanded(
            child: _TenantPaymentHistory(id: widget.tenant!.id),
          )
      ]),
    );
  }

  Widget _buildContents() {
    return SingleChildScrollView(
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: _buildForm(),
        ),
      ),
    );
  }

  Widget _buildForm() {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: _buildFormChildren(),
      ),
    );
  }

  List<Widget> _buildFormChildren() {
    return [
      TextFormField(
        decoration: const InputDecoration(labelText: 'Tenant name'),
        initialValue: _name,
        validator: (value) =>
            (value ?? '').isNotEmpty ? null : 'Name can\'t be empty',
        onSaved: (value) => _name = value,
      ),
      const SizedBox(height: 16),
      TextFormField(
        decoration: const InputDecoration(labelText: 'Rent amount'),
        initialValue: _amount != null ? '$_amount' : null,
        keyboardType: const TextInputType.numberWithOptions(
          signed: false,
          decimal: false,
        ),
        validator: unsignedIntValidator,
        onSaved: (value) => _amount = int.tryParse(value ?? '') ?? 0,
      ),
      const SizedBox(height: 16),
      TextFormField(
        decoration: const InputDecoration(labelText: 'Note'),
        initialValue: _note != null ? '$_note' : null,
        onSaved: (value) => _note = value,
      ),
      const SizedBox(height: 16),
      _TenantListsDropdown(onSelected: (value) => _listId = value),
    ];
  }
}

String? unsignedIntValidator(String? value) {
  if (value == null || value.isEmpty) {
    return 'This field is required.';
  }

  final intValue = int.tryParse(value);

  if (intValue == null || intValue < 0) {
    return 'Please enter a valid positive number';
  }

  return null;
}

class _TenantPaymentHistory extends ConsumerWidget {
  const _TenantPaymentHistory({required this.id});

  final String id;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentUser = ref.watch(authRepositoryProvider).currentUser!;
    final tenantStream = ref
        .watch(tenantsRepositoryProvider)
        .watchTenant(uid: currentUser.uid, id: id);

    return StreamBuilder(
        stream: tenantStream,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const CircularProgressIndicator();
          }
          if (snapshot.hasError) {
            return Text(snapshot.error.toString());
          }
          if (snapshot.hasData) {
            final tenant = snapshot.data!;
            final oldestDate = tenant.createdAt;
            final monthsSince =
                DateUtils.monthDelta(oldestDate, DateTime.now()) + 2;
            return ListView.builder(
              itemBuilder: (context, index) {
                final month = DateUtils.addMonthsToMonthDate(oldestDate, index);
                final payment =
                    tenant.payments[DateFormat('yyyy-MM').format(month)] ?? 0;
                return ListTile(
                  title: Text(DateFormat('MMMM yyyy').format(month)),
                  subtitle: Text("\$$payment"),
                  trailing: IntrinsicWidth(
                    child: Row(
                      mainAxisSize: MainAxisSize
                          .min, // Shrink the Row to the minimum size
                      children: [
                        Text("Has paid",
                            style: Theme.of(context).textTheme.bodyLarge),
                        Checkbox.adaptive(
                          value: payment > 0,
                          onChanged: (value) => {
                            if (value != null)
                              {
                                ref
                                    .read(editTenantScreenControllerProvider
                                        .notifier)
                                    .updatePayment(
                                        id: tenant.id,
                                        month: month,
                                        amountPaid: value ? tenant.amount : 0)
                              }
                          },
                        ),
                      ],
                    ),
                  ),
                );
              },
              itemCount: monthsSince,
            );
          }
          return const Text("No data");
        });
  }
}

class _TenantListsDropdown extends ConsumerWidget {
  const _TenantListsDropdown({this.onSelected});
  final ValueChanged<String?>? onSelected;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tenantLists = ref.watch(listsStreamProvider);
    return tenantLists.when(
      data: (data) => DropdownMenu(
        initialSelection: data.isNotEmpty ? data.first.id : null,
        dropdownMenuEntries: data
            .map((list) => DropdownMenuEntry(value: list.id, label: list.name))
            .toList(),
        onSelected: onSelected,
      ),
      error: (error, stack) => Text(error.toString()),
      loading: () => const CircularProgressIndicator(),
    );
  }
}
