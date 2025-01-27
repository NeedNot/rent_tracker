import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';
import 'package:rent_tracker/src/features/tenants/presentation/edit_tenant_screen/edit_tenant_screen_controller.dart';
import 'package:rent_tracker/src/routing/app_router.dart';

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

  @override
  void initState() {
    super.initState();
    if (widget.tenant != null) {
      _name = widget.tenant?.name;
      _amount = widget.tenant?.amount;
    }
  }

  bool _validateAndSaveForm() {
    final form = _formKey.currentState!;
    if (form.validate()) {
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
                name: _name ?? '',
                amount: _amount ?? 0,
              );
      if (success && mounted) {
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
                    context.pop();
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
      body: _buildContents(),
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
        keyboardAppearance: Brightness.light,
        initialValue: _name,
        validator: (value) =>
            (value ?? '').isNotEmpty ? null : 'Name can\'t be empty',
        onSaved: (value) => _name = value,
      ),
      const SizedBox(height: 16),
      TextFormField(
        decoration: const InputDecoration(labelText: 'Rent amount'),
        keyboardAppearance: Brightness.light,
        initialValue: _amount != null ? '$_amount' : null,
        keyboardType: const TextInputType.numberWithOptions(
          signed: false,
          decimal: false,
        ),
        validator: unsignedIntValidator,
        onSaved: (value) => _amount = int.tryParse(value ?? '') ?? 0,
      ),
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
