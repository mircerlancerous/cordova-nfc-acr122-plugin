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
  
  isOpen: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'isopen', []);
  },
  
  getUSBDevices: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBDevices', []);
  },
  
  getUSBPermission: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBPermissionJS', []);
  }
};