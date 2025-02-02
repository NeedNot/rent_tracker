import 'package:flutter/material.dart';
import 'package:flutter/src/widgets/framework.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:rent_tracker/src/features/authentication/data/firebase_auth_repository.dart';
import 'package:rent_tracker/src/features/lists/data/lists_repository.dart';
import 'package:rent_tracker/src/routing/app_router.dart';

class ListScreen extends ConsumerWidget {
  const ListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentUser = ref.watch(firebaseAuthProvider).currentUser!;
    final listsStream =
        ref.watch(listsRepositoryProvider).watchLists(uid: currentUser.uid);
    return Scaffold(
      appBar: AppBar(
        title: const Text("Lists"),
        centerTitle: true,
      ),
      body: StreamBuilder(
        stream: listsStream,
        builder: (context, snapshot) {
          if (snapshot.hasData) {
            final tenantLists = snapshot.data!;
            return ListView.builder(
              itemCount: tenantLists.length,
              itemBuilder: (context, index) => ListTile(
                title: Text(tenantLists[index].name),
                subtitle: Text(
                  "Shared with ${tenantLists[index].sharedWith.length} people",
                ),
                onTap: () => context.pushNamed(
                  AppRoute.editList.name,
                  pathParameters: {"id": tenantLists[index].id},
                  extra: tenantLists[index],
                ),
              ),
            );
          }
          if (snapshot.hasError) {
            return Text(snapshot.error.toString());
          }
          return const Text("Loading...");
        },
      ),
    );
  }
}
