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
		var onStateChange = function(uid){
			var parts = uid.split(":");
			//if there was an error
			if(parts.length < 8){
				onFail("operation failed");
				return;
			}
			uid = "";
			for(var i=0; i<parts.length-2; i++){
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
	
	transmitAPDU: function(callback, onFail, slotNumber, cmdStr){
		var onStateChange = function(response){
			var parts = Plugin.nfcPlugin.toHexArray(response);
			callback(parts);
		};
		cordova.exec(onStateChange, onFail, 'NfcAcr122Plugin', 'controlDevice', [slotNumber, cmdStr, true]);
	},

	controlDevice: function(callback, onFail, slotNumber, cmdStr){
		var onStateChange = function(response){
			var parts = Plugin.nfcPlugin.toHexArray(response);
			callback(parts);
		};
		cordova.exec(onStateChange, onFail, 'NfcAcr122Plugin', 'controlDevice', [slotNumber, cmdStr, false]);
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
	},

	getDeviceDetails: function(callback, onFail){
		cordova.exec(callback, onFail, 'NfcAcr122Plugin', 'getDeviceDetails', []);
	},
	
	getATR: function(callback, onFail, slotNumber){
		var onStateChange = function(response){
			var parts = Plugin.nfcPlugin.toHexArray(response);
			callback(parts);
		};
		cordova.exec(onStateChange, onFail, 'NfcAcr122Plugin', 'getATR', [slotNumber]);
	},
	
	powerTAG: function(callback, onFail, slotNumber, action){
		var onStateChange = function(response){
			var parts = Plugin.nfcPlugin.toHexArray(response);
			callback(parts);
		};
		cordova.exec(onStateChange, onFail, 'NfcAcr122Plugin', 'powerTAG', [slotNumber, action]);
	},
	
	powerActions: {
		POWERDOWN: 0,
		COLDRESET: 1,
		WARMRESET: 2
	},
	
	toHexArray: function(hexStr){
		var parts = hexStr.split(":");
		for(var i=0; i<parts.length; i++){
			parts[i] = parseInt(parts[i],16);
		}
		return parts;
	}
};