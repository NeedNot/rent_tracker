import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:rent_tracker/src/features/lists/domain/tenant_list.dart';
import 'package:rent_tracker/src/features/lists/presentation/edit_list_screen_controller/edit_list_screen_controller.dart';

class EditListScreen extends ConsumerStatefulWidget {
  const EditListScreen({super.key, this.list});
  final TenantList? list;

  @override
  ConsumerState<EditListScreen> createState() => _EditListScreenState();
}

class _EditListScreenState extends ConsumerState<EditListScreen> {
  Future<void> _delete(BuildContext context) async {}

  @override
  Widget build(BuildContext context) {
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
          const TextButton(
            onPressed: null, //state.isLoading ? null : _submit,
            child: Text('Save'),
          ),
        ],
      ),
      body: const Row(),
    );
  }
}
