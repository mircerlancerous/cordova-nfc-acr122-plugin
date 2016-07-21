var nfcPlugin = {
  stopListen: function(onSuccess, onFail){
    cordova.exec(onSuccess, onFail, 'NfcAcr122Plugin', 'stopListen', []);
  },
  
  startListen: function(callback, onSuccess, onFail){
    document.addEventListener("tag", callback, false);
    cordova.exec(onSuccess, onFail, 'NfcAcr122Plugin', 'startListen', []);
  }
};
