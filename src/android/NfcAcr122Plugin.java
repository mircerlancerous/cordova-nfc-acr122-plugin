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
        
        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        cordova.getActivity().registerReceiver(mReceiver, filter);
        
        // Get USB manager
        usbManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
        // Initialize reader
        reader = new Reader(usbManager);
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
            getUSBDevicesJS(callbackContext);
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
        else if(action.equalsIgnoreCase("enableDevice")){
            enableDeviceJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("getDeviceDetails")){
            getDeviceDetailsJS(callbackContext);
        }
        else if(action.equalsIgnoreCase("getATR")){
            ATRJS(callbackContext, data);
        }
        else if(action.equalsIgnoreCase("powerTAG")){
            powerJS(callbackContext, data);
        }
        else {
            // invalid action
            return false;
        }

        return true;
    }
    
    private boolean enableDevice(){
        // Get attached device
        if(usbDevice != null || findDevice()){
        	// Get permission to use device
        	if(hasUSBPermission() || getUSBPermission()){
	        	return true;
        	}
        }
        return false;
    }
    
    private void enableDeviceJS(CallbackContext callbackContext){
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"");
    	if(!enableDevice()){
    		result = new PluginResult(PluginResult.Status.ERROR,"");
    	}
    	callbackContext.sendPluginResult(result);
    }
    
    private String transmitAPDU(int slotNum, byte[] command) throws Exception{
    	if(command.length == 0){
    		throw new Exception("command is empty");
    	}
    	byte[] response = new byte[300];
		String res = "";
		try{
			int responseLength = reader.transmit(slotNum, command, command.length, response, response.length);
	        res = toHexString(response,responseLength);
        } catch (ReaderException e){
			throw new Exception(e.getMessage());
		}
		return res;
    }
    
    private String controlDevice(int slotNum, byte[] command) throws Exception{
    	if(command.length == 0){
    		throw new Exception("command is empty");
    	}
    	byte[] response = new byte[300];
		String res = "";
    	try{
			int responseLength = reader.control(slotNum, Reader.IOCTL_CCID_ESCAPE, command, command.length, response, response.length);
	        res = toHexString(response,responseLength);
        } catch (ReaderException e){
			throw new Exception(e.getMessage());
		}
        return res;
    }
    	
    private void controlDeviceJS(CallbackContext callbackContext, JSONArray data){
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"command queued");
    	int slotNumber = 0;
    	String commandStr = "";
    	boolean transmit = false;
    	byte[] command = new byte[0];
	    boolean success = false;
    	try{
	    	slotNumber = data.getInt(0);
	    	commandStr = data.getString(1);
	    	transmit = data.getBoolean(2);
	    	command = toByteArray(commandStr);
	    	success = true;
	    } catch(JSONException e){
	    	result = new PluginResult(PluginResult.Status.ERROR,"JSON:"+e.getMessage());
	    }
	    if(success){
			try{
				String response = "";
				if(transmit){
					response = transmitAPDU(slotNumber, command);
				}
				else{
					response = controlDevice(slotNumber, command);
				}
				result = new PluginResult(PluginResult.Status.OK,response);
			} catch (Exception e){
				result = new PluginResult(PluginResult.Status.ERROR,"Reader: "+e.getMessage()+": "+String.valueOf(slotNumber)+": "+commandStr);
			}
		}
		callbackContext.sendPluginResult(result);
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
				} catch (Exception e) {
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
	                byte[] command = new byte[]{ (byte)0xFF, (byte)0xCA, (byte)0x0, (byte)0x0, (byte)0x0 };
	                String uid = controlDevice(slotNumber, command);
	                //if tag removed
	                if(uid == ""){
	                	return;
	                }
                    result = new PluginResult(PluginResult.Status.OK, uid);
                } catch (Exception e) {
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
    	if(reader.isOpened()){
    		return true;
    	}
    	reader.open(usbDevice);
        return reader.isOpened();
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
    
    private void getDeviceDetailsJS(CallbackContext callbackContext){
    	PluginResult result = new PluginResult(PluginResult.Status.ERROR,"device not found");
    	if(usbDevice != null){
    		String json = "";
    		json = "{\"DeviceID\":\""+usbDevice.getDeviceId()+"\",";
    		json += "\"VendorID\":\""+usbDevice.getVendorId()+"\",";
    		json += "\"ProductID\":\""+usbDevice.getProductId()+"\"}";
    		result = new PluginResult(PluginResult.Status.OK,json);
    	}
    	callbackContext.sendPluginResult(result);
    }
    
    private void getUSBDevicesJS(CallbackContext callbackContext){
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
    
    private String ATR(int slotNumber) throws Exception{
    	byte[] response = new byte[0];
    	try{
    		response = reader.getAtr(slotNumber);
    	} catch (ReaderException e){
			throw new Exception(e.getMessage());
		}
    	return toHexString(response);
    }
    
    private void ATRJS(CallbackContext callbackContext, JSONArray data){
    	int slotNumber = 0;
    	boolean success = false;
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"");
    	try{
	    	slotNumber = data.getInt(0);
	    	success = true;
	    } catch(JSONException e){
	    	result = new PluginResult(PluginResult.Status.ERROR,"JSON:"+e.getMessage());
	    }
	    if(success){
	    	try{
	    		String res = ATR(slotNumber);
	    		result = new PluginResult(PluginResult.Status.OK,res);
	    	} catch (Exception e){
	    		result = new PluginResult(PluginResult.Status.ERROR,"Reader:"+e.getMessage());
	    	}
	    }
	    callbackContext.sendPluginResult(result);
    }
    
    private String power(int slotNumber, int action) throws Exception{
    	byte[] response = new byte[0];
    	try{
    		response = reader.power(slotNumber,action);
    	} catch (ReaderException e){
			throw new Exception(e.getMessage());
		}
    	return toHexString(response);
    }
    
    private void powerJS(CallbackContext callbackContext, JSONArray data){
    	int slotNumber = 0;
    	int action = 0;
    	boolean success = false;
    	PluginResult result = new PluginResult(PluginResult.Status.OK,"");
    	try{
	    	slotNumber = data.getInt(0);
	    	action = data.getInt(1);
	    	switch(action){
	    		case 0:action = reader.CARD_POWER_DOWN;break;
	    		case 1:action = reader.CARD_COLD_RESET;break;
	    		case 2:action = reader.CARD_WARM_RESET;break;
	    		default:break;
	    	}
	    	success = true;
	    } catch(JSONException e){
	    	result = new PluginResult(PluginResult.Status.ERROR,"JSON:"+e.getMessage());
	    }
	    if(success){
	    	try{
	    		String res = power(slotNumber,action);
	    		result = new PluginResult(PluginResult.Status.OK,res);
	    	} catch (Exception e){
	    		result = new PluginResult(PluginResult.Status.ERROR,"Reader:"+e.getMessage());
	    	}
	    }
	    callbackContext.sendPluginResult(result);
    }
	
	private String toHexString(byte[] byteArr,int length){
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < length; i++) {
            buff.append(String.format("%02X", byteArr[i]));
            if (i < length - 1) {
                buff.append(":");
            }
        }
        return buff.toString();
	}
	
    private byte[] toByteArray(String hexString) {

        int hexStringLength = hexString.length();
        byte[] byteArray = null;
        int count = 0;
        char c;
        int i;

        // Count number of hex characters
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f') {
                count++;
            }
        }

        byteArray = new byte[(count + 1) / 2];
        boolean first = true;
        int len = 0;
        int value;
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                value = c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                value = c - 'a' + 10;
            } else {
                value = -1;
            }

            if (value >= 0) {

                if (first) {

                    byteArray[len] = (byte) (value << 4);

                } else {

                    byteArray[len] |= value;
                    len++;
                }

                first = !first;
            }
        }

        return byteArray;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {

                synchronized (this) {

                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						//permission granted
                        
                    } else {
						//permission denied
						
                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				/*
                synchronized (this) {

                    // Update reader list
                    mReaderAdapter.clear();
                    for (UsbDevice device : mManager.getDeviceList().values()) {
                        if (mReader.isSupported(device)) {
                            mReaderAdapter.add(device.getDeviceName());
                        }
                    }

                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (device != null && device.equals(mReader.getDevice())) {
						//close the associated reader
						
                    }
                }
                */
            }
        }
	};
}
