import 'package:flutter/material.dart';
import 'package:rent_tracker/src/features/tenants/domain/tenant.dart';

class CreateTenantScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Create tenant"),
        centerTitle: true,
      ),
      body: Text("Create tenant"),
    );
  }
}
