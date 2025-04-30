import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Native Bridge Demo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green),
        useMaterial3: true,
      ),
      home: NativeBridgeScreen(),
    );
  }
}

class NativeBridgeScreen extends StatefulWidget {
  @override
  _NativeBridgeScreenState createState() => _NativeBridgeScreenState();
}

class _NativeBridgeScreenState extends State<NativeBridgeScreen> {
  static const MethodChannel _methodChannel = MethodChannel('native_bridge/method');
  static const EventChannel _eventChannel = EventChannel('native_bridge/event');

  String _batteryLevel = 'Unknown';
  String _networkType = 'Unknown';
  String _sensorData = 'Waiting for data...';
  String _cameraStatus = '';
  String _galleryStatus = '';
  String _flashlightStatus = 'Off';

  @override
  void initState() {
    super.initState();
    _listenToSensorData();
  }

  void _listenToSensorData() {
    _eventChannel.receiveBroadcastStream().listen(
          (dynamic event) {
        setState(() {
          _sensorData = event.toString();
        });
      },
      onError: (error) {
        setState(() {
          _sensorData = 'Sensor error: ${error.toString()}';
        });
      },
    );
  }

  Future<void> _getBatteryLevel() async {
    try {
      final int result = await _methodChannel.invokeMethod('getBatteryLevel');
      setState(() {
        _batteryLevel = '$result%';
      });
    } on PlatformException catch (e) {
      setState(() {
        _batteryLevel = "Failed to get battery level: '${e.message}'";
      });
    }
  }

  Future<void> _getNetworkType() async {
    try {
      final String result = await _methodChannel.invokeMethod('getNetworkType');
      setState(() {
        _networkType = result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _networkType = "Failed to get network type: '${e.message}'";
      });
    }
  }

  Future<void> _openCamera() async {
    try {
      final String result = await _methodChannel.invokeMethod('openCamera');
      setState(() {
        _cameraStatus = result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _cameraStatus = "Camera error: '${e.message}'";
      });
    }
  }

  Future<void> _openGallery() async {
    try {
      final String result = await _methodChannel.invokeMethod('openGallery');
      setState(() {
        _galleryStatus = result;
      });
    } on PlatformException catch (e) {
      setState(() {
        _galleryStatus = "Gallery error: '${e.message}'";
      });
    }
  }

  Future<void> _toggleFlashlight() async {
    try {
      final String result = await _methodChannel.invokeMethod('toggleFlashlight');
      setState(() {
        _flashlightStatus = _flashlightStatus == 'Off' ? 'On' : 'Off';
      });
    } on PlatformException catch (e) {
      setState(() {
        _flashlightStatus = "Failed to toggle flashlight: '${e.message}'";
      });
    }
  }

  Widget _buildStatusCard(String title, String value, VoidCallback onPressed) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 10),
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title,
                style: TextStyle(
                    fontSize: 18, fontWeight: FontWeight.bold, color: Colors.green[700])),
            const SizedBox(height: 8),
            Text(value, style: TextStyle(fontSize: 16)),
            const SizedBox(height: 10),
            Align(
              alignment: Alignment.centerRight,
              child: ElevatedButton.icon(
                onPressed: onPressed,
                icon: Icon(Icons.play_arrow),
                label: Text('Run'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSensorCard() {
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
            Text("Sensor Data (Accelerometer)",
                style: TextStyle(
                    fontSize: 18, fontWeight: FontWeight.bold, color: Colors.green[800])),
            const SizedBox(height: 10),
            Text(_sensorData, style: TextStyle(fontSize: 16)),
          ],
        ),
      ),
    );
  }

  Widget _buildFlashlightCard() {
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
            Text("Flashlight Status",
                style: TextStyle(
                    fontSize: 18, fontWeight: FontWeight.bold, color: Colors.green[800])),
            const SizedBox(height: 10),
            Text(_flashlightStatus, style: TextStyle(fontSize: 16)),
            const SizedBox(height: 10),
            Align(
              alignment: Alignment.centerRight,
              child: ElevatedButton.icon(
                onPressed: _toggleFlashlight,
                icon: Icon(Icons.flashlight_on),
                label: Text('Toggle'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Native Bridge via Channels'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildStatusCard("Battery Level", _batteryLevel, _getBatteryLevel),
          _buildStatusCard("Network Type", _networkType, _getNetworkType),
          _buildStatusCard("Camera Status", _cameraStatus, _openCamera),
          _buildStatusCard("Gallery Status", _galleryStatus, _openGallery),
          _buildSensorCard(),
          _buildFlashlightCard(),
        ],
      ),
    );
  }
}
