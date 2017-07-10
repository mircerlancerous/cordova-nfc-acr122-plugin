package com.otb.cordova.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class NfcAcr122Plugin extends CordovaPlugin  {
    private UsbManager usbManager;
    private UsbDevice usbDevice;

    private Reader reader;
    private PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    
    private CallbackContext callback = null;
    
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        
        // Get USB manager
        usbManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
        // Initialize reader
        reader = new Reader(usbManager);
        // Get attached device
        if(findDevice()){
        	// Get permission to use device
        	if(getUSBPermission()){
        		// Open the USB port
        		open();
        	}
        }
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        
        if (action.equalsIgnoreCase("uidListen")) {
            uidListenJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("listen")){
            listenJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("open")){
            openJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("close")){
            closeJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("isOpen")){
            isOpenJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("controlDevice")){
            controlDeviceJS(callbackContext, data);
        }
        else if(action.equalsIgnoreCase("getUSBDevices")){
            getUSBDevices(callbackContext);
        }
        else if(action.equalsIgnoreCase("getUSBPermission")){
            getUSBPermissionJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("hasUSBPermission")){
            hasUSBPermissionJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("hasUSBDevice")){
            hasUSBDeviceJS(callbackContext);
        }
        else {
            // invalid action
            return false;
        }

        return true;
    }
    
    private String controlDevice(int slotNum, byte[] command){
    	byte[] response = new byte[300];
		int responseLength = reader.control(slotNum, Reader.IOCTL_CCID_ESCAPE, command, command.length, response, response.length);
		StringBuffer buff = new StringBuffer();
        for (int i = 0; i < responseLength; i++) {
            buff.append(String.format("%02X", response[i]));
            if (i < responseLength - 1) {
                buff.append(":");
            }
        }
        return buff.toString();
    }
    	
    private void controlDeviceJS(CallbackContext callbackContext, JSONArray data){
    	int slotNumber = data.getInt(0);
    	byte[] command = new byte[data.length()];
    	for(int i=1; i<data.length(); i++){
    		command[i] = (byte)data.getInt(i);
    	}
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"command queued");
		try{
			String response = controlDevice(slotNumber, command);
			result = new PluginResult(PluginResult.Status.OK,new String(response));
		} catch (ReaderException e){
			result = new PluginResult(PluginResult.Status.ERROR,e.getMessage());
		}
		callback.sendPluginResult(result);
	}
	
	private void listenJS(CallbackContext callbackContext){
		open();
		
		reader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int slotNumber, int prevState, int currState) {
				PluginResult result = new PluginResult(PluginResult.Status.OK,"state change detected");
                try {
					StringBuffer state = new StringBuffer();
					state.append(String.valueOf(slotNumber));
					state.append(":");
					state.append(String.valueOf(prevState));
					state.append(":");
					state.append(String.valueOf(currState));
					result = new PluginResult(PluginResult.Status.OK,state.toString());
				} catch (ReaderException e) {
					result = new PluginResult(PluginResult.Status.ERROR,e.getMessage());
				}
				result.setKeepCallback(true);
				callback.sendPluginResult(result);
			}
		});
		
		PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT,"");
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        callback = callbackContext;
	}
	
	private void uidListenJS(CallbackContext callbackContext){
    	open();
    	
        reader.setOnStateChangeListener(new Reader.OnStateChangeListener(){
            @Override
            public void onStateChange(int slotNumber, int prevState, int currState){
				PluginResult result = new PluginResult(PluginResult.Status.OK,"state change detected");
                try{
	                byte[] command = new byte[]{
	                	(byte)0xFF,
	                	(byte)0xCA,
	                	(byte)0x0,
	                	(byte)0x0,
	                	(byte)0x0
	                };
	                String uid = controlDevice(slotNumber, command);
                    result = new PluginResult(PluginResult.Status.OK, uid);
                } catch (ReaderException e) {
                	result = new PluginResult(PluginResult.Status.ERROR,e.getMessage());
                }
                result.setKeepCallback(true);
                callback.sendPluginResult(result);
            }
        });
        
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT,"");
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        callback = callbackContext;
    }
    
    private boolean open(){
    	if(!hasUSBPermission()){
    		return false;
    	}
    	if(reader.isOpened()){
    		return true;
    	}
    	reader.open(usbDevice);
        if(!reader.isOpened()){
        	return false;
        }
        return true;
    }
    
    private void openJS(CallbackContext callbackContext){
        PluginResult result = new PluginResult(PluginResult.Status.OK,"");
        if(!open()){
			result = new PluginResult(PluginResult.Status.ERROR,"reader not opened");
        }
    	callbackContext.sendPluginResult(result);
    }
    
    private void isOpenJS(CallbackContext callbackContext){
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"");
    	if(!reader.isOpened()){
    		result = new PluginResult(PluginResult.Status.ERROR,"reader not opened");
    	}
    	callbackContext.sendPluginResult(result);
    }
    
	private void close(){
		if(reader.isOpened()){
			reader.close();
		}
	}
	
    private void closeJS(CallbackContext callbackContext){
        close();
        PluginResult result = new PluginResult(PluginResult.Status.OK,"");
    	callbackContext.sendPluginResult(result);
    }
    
    private boolean findDevice(){
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            if(reader.isSupported(device)){
            	usbDevice = device;
                return true;
            }
        }
        return false;
    }
    
    private boolean hasUSBPermission(){
    	if(usbDevice == null){
    		return false;
    	}
    	if(!usbManager.hasPermission(usbDevice)){
    		return false;
    	}
    	return true;
    }
    
    private void hasUSBDeviceJS(CallbackContext callbackContext){
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"");
    	if(usbDevice == null && !findDevice()){
    		result = new PluginResult(PluginResult.Status.ERROR,"device not found");
    	}
    	callbackContext.sendPluginResult(result);
    }
    
    private void hasUSBPermissionJS(CallbackContext callbackContext){
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"");
    	
    	if(!hasUSBPermission()){
    		result = new PluginResult(PluginResult.Status.ERROR,"usb permission not granted");
    	}
    	
    	callbackContext.sendPluginResult(result);
    }
    
    private boolean getUSBPermission(){
        mPermissionIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(usbDevice,mPermissionIntent);
        return hasUSBPermission();
    }
    
    private void getUSBPermissionJS(CallbackContext callbackContext){
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"");
    	
    	if(!hasUSBPermission()){
    		if(usbDevice == null && !findDevice()){
    			result = new PluginResult(PluginResult.Status.ERROR,"device not found");
    		}
    		else if(!getUSBPermission()){
    			result = new PluginResult(PluginResult.Status.ERROR,"usb permission not granted");
    		}
    	}
		
		callbackContext.sendPluginResult(result);
    }
    
    private void getUSBDevices(CallbackContext callbackContext){
		String outStr = "";
		HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while(deviceIterator.hasNext()){
			UsbDevice device = deviceIterator.next();
			outStr += "<br/>Model = "+device.getDeviceName();
			outStr += "<br/>DeviceID = "+device.getDeviceId();
			outStr += "<br/>Vendor = "+device.getVendorId();
			outStr += "<br/>Product = "+device.getProductId();
			outStr += "<br/>Class = "+device.getDeviceClass();
			outStr += "<br/>Subclass = "+device.getDeviceSubclass();
			outStr += "<br/>Readable = "+reader.isSupported(device);
		}

		PluginResult result = new PluginResult(PluginResult.Status.OK,outStr);
		callbackContext.sendPluginResult(result);
    }
}
