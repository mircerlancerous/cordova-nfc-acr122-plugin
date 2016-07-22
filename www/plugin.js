Plugin.nfcPlugin = {
  startListen: function(callback, onFail){
    //document.addEventListener("tag", callback, false);
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'startListen', []);
  },
  
  getUSBDevices: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBDevices', []);
  },
  
  getUSBPermission: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBPermission', []);
  }
};
