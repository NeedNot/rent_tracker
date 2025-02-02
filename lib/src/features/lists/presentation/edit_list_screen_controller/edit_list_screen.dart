import 'package:email_validator/email_validator.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
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
  List<String>? _sharedWith;

  @override
  void initState() {
    super.initState();
    _name = widget.list?.name;
    _sharedWith = widget.list?.sharedWith;
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
          .submit(
              oldList: widget.list,
              name: _name!,
              sharedWith: _sharedWith ?? []);
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
    final uid = ref.watch(firebaseAuthProvider).currentUser!.uid;
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.list != null ? "Edit list" : "Create list"),
        actions: <Widget>[
          if (widget.list != null && widget.list?.ownerId == uid)
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
      _ShareWithEmail(
        sharedWith: _sharedWith ?? [],
        onUpdateList: (list) => _sharedWith = list,
      )
    ];
  }
}

class _ShareWithEmail extends StatefulWidget {
  const _ShareWithEmail({required this.sharedWith, required this.onUpdateList});
  final List<String> sharedWith;
  final Function(List<String>) onUpdateList;
  @override
  State<_ShareWithEmail> createState() => _ShareWithEmailState();
}

class _ShareWithEmailState extends State<_ShareWithEmail> {
  final _formKey = GlobalKey<FormState>();
  late List<String> _sharedWith;

  @override
  void initState() {
    super.initState();
    _sharedWith = List.from(widget.sharedWith); // Ensure it's a separate list
  }

  bool _validateAndSaveForm() {
    final form = _formKey.currentState!;
    if (form.validate()) {
      form.save();
      return true;
    }
    return false;
  }

  void _removeEmail(int index) {
    setState(() {
      _sharedWith = List.from(_sharedWith)..removeAt(index);
      widget.onUpdateList(_sharedWith);
    });
  }

  void _addEmail(String value) {
    setState(() {
      _sharedWith.add(value);
      widget.onUpdateList(_sharedWith);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  decoration:
                      const InputDecoration(labelText: 'Share with email'),
                  validator: (value) {
                    if (!EmailValidator.validate(value ?? '')) {
                      return "Enter a valid email address";
                    }
                    if (widget.sharedWith.contains(value)) {
                      return "Email already in shared list";
                    }
                    return null;
                  },
                  onSaved: (value) {
                    _addEmail(value!);
                    _formKey.currentState!.reset();
                  },
                ),
              ),
              OutlinedButton(
                onPressed: _validateAndSaveForm,
                child: const Text("Add"),
              ),
            ],
          ),
          ListView.builder(
            shrinkWrap: true,
            itemCount: _sharedWith.length,
            itemBuilder: (context, index) => ListTile(
              title: Text(_sharedWith[index]),
              trailing: IconButton(
                onPressed: () => _removeEmail(index),
                icon: const Icon(Icons.remove),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
