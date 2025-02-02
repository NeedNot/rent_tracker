import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rent_tracker/src/features/lists/domain/tenant_list.dart';
import 'package:rent_tracker/src/features/lists/presentation/edit_list_screen_controller/edit_list_screen_controller.dart';

class EditListScreen extends ConsumerStatefulWidget {
  const EditListScreen({super.key, this.list});
  final TenantList? list;

  @override
  ConsumerState<EditListScreen> createState() => _EditListScreenState();
}

class _EditListScreenState extends ConsumerState<EditListScreen> {
  final _formKey = GlobalKey<FormState>();

  String? _name;

  @override
  void initState() {
    super.initState();
    _name = widget.list?.name;
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
      final success = await ref
          .read(editListScrenControllerProvider.notifier)
          .submit(oldList: widget.list, name: _name!, sharedWith: []);
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
            title: const Text('Delete list?'),
            content: const Text('Are you sure you want to delete this list?'),
            actions: <Widget>[
              TextButton(
                child: const Text('Cancel'),
                onPressed: () => context.pop(),
              ),
              TextButton(
                child: const Text('Delete'),
                onPressed: () async {
                  final success = await ref
                      .read(editListScrenControllerProvider.notifier)
                      .delete(widget.list!.id);
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
      editListScrenControllerProvider,
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
    final state = ref.watch(editListScrenControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.list != null ? "Edit list" : "Create list"),
        actions: <Widget>[
          if (widget.list != null)
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
        buildContents(),
      ]),
    );
  }

  Widget buildContents() {
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
    ];
  }
}
