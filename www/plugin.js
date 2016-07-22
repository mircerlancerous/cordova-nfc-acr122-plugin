Plugin.nfcPlugin = {
  stopListen: function(onSuccess, onFail){
    cordova.exec(onSuccess, onFail, 'NfcAcr122Plugin', 'stopListen', []);
  },
  
  startListen: function(callback, onFail){
    //document.addEventListener("tag", callback, false);
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'startListen', []);
  },
  
  getUSBDevices: function(callback, onFail){
    cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBDevices', []);
  }
};
