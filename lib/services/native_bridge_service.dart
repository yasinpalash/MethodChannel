import 'package:flutter/services.dart';

class NativeBridgeService {
  static final NativeBridgeService _instance = NativeBridgeService._internal();

  factory NativeBridgeService() {
    return _instance;
  }

  NativeBridgeService._internal();

  static const MethodChannel _methodChannel = MethodChannel('native_bridge/method');
  static const EventChannel _accelerometerEventChannel = EventChannel('native_bridge/event');
  static const EventChannel _locationEventChannel = EventChannel('native_bridge/location_event');
  static const EventChannel _chargingEventChannel = EventChannel('native_bridge/charging_event');

  // Method channel functions
  Future<int> getBatteryLevel() async {
    try {
      final int result = await _methodChannel.invokeMethod('getBatteryLevel');
      return result;
    } on PlatformException catch (e) {
      throw Exception("Failed to get battery level: '${e.message}'");
    }
  }

  Future<String> getNetworkType() async {
    try {
      final String result = await _methodChannel.invokeMethod('getNetworkType');
      return result;
    } on PlatformException catch (e) {
      throw Exception("Failed to get network type: '${e.message}'");
    }
  }

  Future<String> openCamera() async {
    try {
      final String result = await _methodChannel.invokeMethod('openCamera');
      return result;
    } on PlatformException catch (e) {
      throw Exception("Camera error: '${e.message}'");
    }
  }

  Future<String> openGallery() async {
    try {
      final String result = await _methodChannel.invokeMethod('openGallery');
      return result;
    } on PlatformException catch (e) {
      throw Exception("Gallery error: '${e.message}'");
    }
  }

  Future<void> toggleFlashlight() async {
    try {
      await _methodChannel.invokeMethod('toggleFlashlight');
    } on PlatformException catch (e) {
      throw Exception("Failed to toggle flashlight: '${e.message}'");
    }
  }

  // Event channel streams
  Stream<Map<String, dynamic>> get accelerometerStream {
    return _accelerometerEventChannel
        .receiveBroadcastStream()
        .map<Map<String, dynamic>>((dynamic event) => Map<String, dynamic>.from(event));
  }

  Stream<Map<String, dynamic>> get locationStream {
    return _locationEventChannel
        .receiveBroadcastStream()
        .map<Map<String, dynamic>>((dynamic event) => Map<String, dynamic>.from(event));
  }

  Stream<Map<String, dynamic>> get chargingStream {
    return _chargingEventChannel
        .receiveBroadcastStream()
        .map<Map<String, dynamic>>((dynamic event) => Map<String, dynamic>.from(event));
  }
}
