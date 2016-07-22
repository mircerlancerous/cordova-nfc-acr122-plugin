Plugin.nfcPlugin = {
  startListen: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'listen', []);
  },
  
  open: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'open', []);
  },
  
  close: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'close', []);
  },
  
  test: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'test', []);
  },
  
  getUSBDevices: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBDevices', []);
  },
  
  getUSBPermission: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBPermission', []);
  },
  
  getUSBPermission2: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBPermission2', []);
  }
};
