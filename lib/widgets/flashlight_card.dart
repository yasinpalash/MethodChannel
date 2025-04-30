import 'package:flutter/material.dart';

class FlashlightCard extends StatelessWidget {
  final String status;
  final VoidCallback onToggle;

  const FlashlightCard({
    Key? key,
    required this.status,
    required this.onToggle,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 10),
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      color: Colors.green.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              "Flashlight Status",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Colors.green[800],
              ),
            ),
            const SizedBox(height: 10),
            Text(status, style: TextStyle(fontSize: 16)),
            const SizedBox(height: 10),
            Align(
              alignment: Alignment.centerRight,
              child: ElevatedButton.icon(
                onPressed: onToggle,
                icon: Icon(Icons.flashlight_on),
                label: Text('Toggle'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
