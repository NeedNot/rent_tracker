import 'package:flutter/material.dart';

class CreateTenantScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Create tenant"),
        centerTitle: true,
      ),
      body: Center(child: const Text("Create tenant")),
    );
  }
}
