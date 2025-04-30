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
  static const EventChannel _accelerometerEventChannel = EventChannel('native_bridge/event');
  static const EventChannel _locationEventChannel = EventChannel('native_bridge/location_event');
  static const EventChannel _chargingEventChannel = EventChannel('native_bridge/charging_event');

  String _batteryLevel = 'Unknown';
  String _networkType = 'Unknown';
  String _sensorData = 'Waiting for data...';
  String _locationData = 'Waiting for location...';
  String _chargingStatus = 'Waiting for charging status...';
  String _cameraStatus = '';
  String _galleryStatus = '';
  String _flashlightStatus = 'Off';

  @override
  void initState() {
    super.initState();
    _listenToSensorData();
    _listenToLocationData();
    _listenToChargingStatus();
  }

  void _listenToSensorData() {
    _accelerometerEventChannel.receiveBroadcastStream().listen(
          (dynamic event) {
        setState(() {
          _sensorData = 'X: ${event['x'].toStringAsFixed(2)}, '
              'Y: ${event['y'].toStringAsFixed(2)}, '
              'Z: ${event['z'].toStringAsFixed(2)}';
        });
      },
      onError: (error) {
        setState(() {
          _sensorData = 'Sensor error: ${error.toString()}';
        });
      },
    );
  }

  void _listenToLocationData() {
    _locationEventChannel.receiveBroadcastStream().listen(
          (dynamic event) {
        setState(() {
          _locationData = 'Lat: ${event['latitude'].toStringAsFixed(6)}, '
              'Lng: ${event['longitude'].toStringAsFixed(6)}\n'
              'Alt: ${event['altitude'].toStringAsFixed(1)}m, '
              'Acc: ${event['accuracy'].toStringAsFixed(1)}m';
        });
      },
      onError: (error) {
        setState(() {
          if (error.message.toString().contains('PERMISSION_DENIED') ||
              error.message.toString().contains('permission denied')) {
            _locationData = 'Location permission denied. Please grant location permissions in your device settings.';
          } else {
            _locationData = 'Location error: ${error.message}';
          }
        });
      },
    );
  }

  void _listenToChargingStatus() {
    _chargingEventChannel.receiveBroadcastStream().listen(
          (dynamic event) {
        setState(() {
          if (event['status'] == 'charging' || event['status'] == 'connected') {
            _chargingStatus = 'Charging (${event['level']?.toStringAsFixed(1)}%)';
            if (event['source'] != null) {
              _chargingStatus += '\nSource: ${event['source']}';
            }
          } else {
            _chargingStatus = 'Not charging (${event['level']?.toStringAsFixed(1)}%)';
          }
        });
      },
      onError: (error) {
        setState(() {
          _chargingStatus = 'Charging status error: ${error.message}';
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

  Widget _buildStreamCard(String title, String value, {Color? color, VoidCallback? onAction, String? actionLabel}) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 10),
      elevation: 3,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      color: color ?? Colors.green.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title,
                style: TextStyle(
                    fontSize: 18, fontWeight: FontWeight.bold, color: Colors.green[800])),
            const SizedBox(height: 10),
            Text(value, style: TextStyle(fontSize: 16)),
            if (onAction != null && actionLabel != null) ...[
              const SizedBox(height: 10),
              Align(
                alignment: Alignment.centerRight,
                child: ElevatedButton(
                  onPressed: onAction,
                  child: Text(actionLabel),
                ),
              ),
            ],
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

  Future<void> _openAppSettings() async {
    // This is a placeholder. In a real app, you would use a plugin like app_settings or permission_handler
    // to open the app settings page.
    // For example with permission_handler:
    // await openAppSettings();

    // For now, we'll just show a dialog
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Open Settings'),
        content: Text('To use location features, please open your device settings and grant location permissions to this app.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text('OK'),
          ),
        ],
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

          _buildStreamCard(
            "Location Data",
            _locationData,
            color: Colors.blue.shade50,
            onAction: _locationData.contains('permission denied') ? _openAppSettings : null,
            actionLabel: _locationData.contains('permission denied') ? 'Open Settings' : null,
          ),
          _buildStreamCard("Charging Status", _chargingStatus, color: Colors.amber.shade50),
          _buildFlashlightCard(),
          _buildStreamCard("Sensor Data (Accelerometer)", _sensorData),
        ],
      ),
    );
  }
}
