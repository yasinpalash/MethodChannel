import 'package:flutter/material.dart';
import '../services/native_bridge_service.dart';
import '../widgets/flashlight_card.dart';
import '../widgets/status_card.dart';
import '../widgets/stream_card.dart';


class NativeBridgeScreen extends StatefulWidget {
  const NativeBridgeScreen({super.key});

  @override
  _NativeBridgeScreenState createState() => _NativeBridgeScreenState();
}

class _NativeBridgeScreenState extends State<NativeBridgeScreen> {
  final NativeBridgeService _nativeBridge = NativeBridgeService();

  String _batteryLevel = 'Unknown';
  String _networkType = 'Unknown';
  String _sensorData = 'Waiting for data...';
  String _locationData = 'Waiting for location...';
  String _locationAddress = '';
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
    _nativeBridge.accelerometerStream.listen(
          (Map<String, dynamic> event) {
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
    _nativeBridge.locationStream.listen(
          (Map<String, dynamic> event) {
        setState(() {
          // Update coordinates
          _locationData = 'Lat: ${event['latitude'].toStringAsFixed(6)}, '
              'Lng: ${event['longitude'].toStringAsFixed(6)}\n'
              'Alt: ${event['altitude'].toStringAsFixed(1)}m, '
              'Acc: ${event['accuracy'].toStringAsFixed(1)}m';

          // Update address if available
          if (event['address'] != null) {
            _locationAddress = '📍 ${event['address']}';
          }
        });
      },
      onError: (error) {
        setState(() {
          if (error.message.toString().contains('PERMISSION_DENIED') ||
              error.message.toString().contains('permission denied')) {
            _locationData = 'Location permission denied. Please grant location permissions in your device settings.';
            _locationAddress = '';
          } else {
            _locationData = 'Location error: ${error.message}';
            _locationAddress = '';
          }
        });
      },
    );
  }

  void _listenToChargingStatus() {
    _nativeBridge.chargingStream.listen(
          (Map<String, dynamic> event) {
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
      final int result = await _nativeBridge.getBatteryLevel();
      setState(() {
        _batteryLevel = '$result%';
      });
    } catch (e) {
      setState(() {
        _batteryLevel = e.toString();
      });
    }
  }

  Future<void> _getNetworkType() async {
    try {
      final String result = await _nativeBridge.getNetworkType();
      setState(() {
        _networkType = result;
      });
    } catch (e) {
      setState(() {
        _networkType = e.toString();
      });
    }
  }

  Future<void> _openCamera() async {
    try {
      final String result = await _nativeBridge.openCamera();
      setState(() {
        _cameraStatus = result;
      });
    } catch (e) {
      setState(() {
        _cameraStatus = e.toString();
      });
    }
  }

  Future<void> _openGallery() async {
    try {
      final String result = await _nativeBridge.openGallery();
      setState(() {
        _galleryStatus = result;
      });
    } catch (e) {
      setState(() {
        _galleryStatus = e.toString();
      });
    }
  }

  Future<void> _toggleFlashlight() async {
    try {
      await _nativeBridge.toggleFlashlight();
      setState(() {
        _flashlightStatus = _flashlightStatus == 'Off' ? 'On' : 'Off';
      });
    } catch (e) {
      setState(() {
        _flashlightStatus = e.toString();
      });
    }
  }

  Future<void> _openAppSettings() async {
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
          StatusCard(
            title: "Battery Level",
            value: _batteryLevel,
            onPressed: _getBatteryLevel,
          ),
          StatusCard(
            title: "Network Type",
            value: _networkType,
            onPressed: _getNetworkType,
          ),
          StatusCard(
            title: "Camera Status",
            value: _cameraStatus,
            onPressed: _openCamera,
          ),
          StatusCard(
            title: "Gallery Status",
            value: _galleryStatus,
            onPressed: _openGallery,
          ),

          StreamCard(
            title: "Location Data",
            value: _locationData,
            color: Colors.blue.shade50,
            subtitle: _locationAddress,
            onAction: _locationData.contains('permission denied') ? _openAppSettings : null,
            actionLabel: _locationData.contains('permission denied') ? 'Open Settings' : null,
          ),
          StreamCard(
            title: "Charging Status",
            value: _chargingStatus,
            color: Colors.amber.shade50,
          ),
          FlashlightCard(
            status: _flashlightStatus,
            onToggle: _toggleFlashlight,
          ),
          StreamCard(
            title: "Sensor Data (Accelerometer)",
            value: _sensorData,
          ),
        ],
      ),
    );
  }
}
