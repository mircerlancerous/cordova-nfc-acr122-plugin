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
        
        if (action.equalsIgnoreCase("listen")) {
            listen(callbackContext);
        }
        else if(action.equalsIgnoreCase("open")){
            openJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("close")){
            closeJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("isopen")){
            isopenJS(callbackContext);
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
        else {
            // invalid action
            return false;
        }

        return true;
    }
    
    private void listen(CallbackContext callbackContext){
    	open();
        reader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int slotNumber, int prevState, int currState) {
                byte[] sendBuffer = new byte[]{ (byte)0xFF, (byte)0xCA, (byte)0x0, (byte)0x0, (byte)0x0};
                byte[] receiveBuffer = new byte[16];
				PluginResult result = new PluginResult(PluginResult.Status.OK,"state change detected");
                try {
                    int byteCount = reader.control(slotNumber, Reader.IOCTL_CCID_ESCAPE, sendBuffer, sendBuffer.length, receiveBuffer, receiveBuffer.length);
                    //int MIFARE_CLASSIC_UID_LENGTH = 4;
                    StringBuffer uid = new StringBuffer();
                    for (int i = 0; i < (byteCount - 2); i++) {
                        uid.append(String.format("%02X", receiveBuffer[i]));
                        if (i < byteCount - 3) {
                            uid.append(":");
                        }
                    }

                    result = new PluginResult(PluginResult.Status.OK, uid.toString());
                    
                }
                catch (ReaderException e) {
                	result = new PluginResult(PluginResult.Status.ERROR,e.getMessage());
                }
                result.setKeepCallback(true);
                callback.sendPluginResult(result);
            }
        });
        
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT,null);
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
        PluginResult result = new PluginResult(PluginResult.Status.OK,null);
        if(!open()){
			result = new PluginResult(PluginResult.Status.ERROR,"reader not opened");
        }
    	callbackContext.sendPluginResult(result);
    }
    
    private void isopenJS(CallbackContext callbackContext){
    	PluginResult result = new PluginResult(PluginResult.Status.OK,null);
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
        PluginResult result = new PluginResult(PluginResult.Status.OK,null);
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
    
    private void hasUSBPermissionJS(CallbackContext callbackContext){
    	PluginResult result = new PluginResult(PluginResult.Status.OK,null);
    	
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
    	PluginResult result = new PluginResult(PluginResult.Status.OK,null);
    	
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
