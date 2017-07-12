Plugin.nfcPlugin = {
	open: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'open', []);
	},

	close: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'close', []);
	},

	isOpen: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'isOpen', []);
	},

	uidListen: function(callback, onFail){
		var onStateChange = function(uid){console.log("uid:"+uid);
			var parts = uid.split(":");
			uid = "";
			for(var i=0; i<parts.length; i++){
				uid += parts[i];
			}
			callback(uid);
		};
		cordova.exec(onStateChange, onFail, 'NfcAcr122Plugin', 'uidListen', []);
	},

	listen: function(callback, onFail){
		var onStateChange = function(state){
			state = state.split(":");
			state = {
				slotNumber: parseInt(state[0]),
				prevState: parseInt(state[1]),
				currState: parseInt(state[2])
			};
			callback(state);
		};
		cordova.exec(onStateChange, onFail, 'NfcAcr122Plugin', 'listen', []);
	},

	controlDevice: function(callback, onFail, slotNumber, b1, b2, b3, b4, b5){
		var onStateChange = function(response){
			var parts = response.split(":");
			for(var i=0; i<parts.length; i++){
				parts[i] = parseInt(parts[i],16);
			}
			callback(parts);
		};
		cordova.exec(onStateChange, onFail, 'NfcAcr122Plugin', 'controlDevice', [slotNumber, b1, b2, b3, b4, b5]);
	},

	getUSBDevices: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBDevices', []);
	},

	getUSBPermission: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getUSBPermission', []);
	},

	hasUSBPermission: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'hasUSBPermission', []);
	},

	hasUSBDevice: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'hasUSBDevice', []);
	},

	enableDevice: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'enableDevice', []);
	}
};