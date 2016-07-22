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
    
    private CallbackContext callback = null;
    
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        
        // Get USB manager
        usbManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
        // Initialize reader
        reader = new Reader(usbManager);
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        
        if (action.equalsIgnoreCase("listen")) {
            listen(callbackContext);
        }
        else if(action.equalsIgnoreCase("open")){
            open(callbackContext);
        }
        else if(action.equalsIgnoreCase("close")){
            close(callbackContext);
        }
        else if(action.equalsIgnoreCase("test")){
            test(callbackContext);
        }
        else if(action.equalsIgnoreCase("getUSBDevices")){
            getUSBDevices(callbackContext);
        }
        else if(action.equalsIgnoreCase("getUSBPermission")){
            getUSBPermission(callbackContext);
        }
        else {
            // invalid action
            return false;
        }

        return true;
    }
    
    private void listen(CallbackContext callbackContext){
        reader.setOnStateChangeListener(new reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {
        
                callback.success("state change detected: slotNum="+slotNum);
                
            }
        });
        
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        callback = callbackContext;
    }
    
    private void open(CallbackContext callbackContext){
        if(usbDevice == null){
            usbDevice = findDevice();
        }
        reader.open(usbDevice);
        if(reader.isOpened()){
            callbackContext.success("reader opened!");
        }
        else{
            callbackContext.error("reader not opened");
        }
    }
    
    private void close(CallbackContext callbackContext){
        if(reader.isOpened()){
            reader.close();
        }
        callbackContext.success("reader closed");
    }
    
    private UsbDevice findDevice(){
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            if(reader.isSupported(device)){
                return device;
            }
        }
        return null;
    }

    private void test(CallbackContext callbackContext) {
        callbackContext.success("plugin works");
    }
    
    private void getUSBDevices(CallbackContext callbackContext){
        
        String outStr = "";
        
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();

            //usbManager.requestPermission(device, mPermissionIntent);
            outStr += "<br/>Model = "+device.getDeviceName();

            outStr += "<br/>DeviceID = "+device.getDeviceId();
            outStr += "<br/>Vendor = "+device.getVendorId();
            outStr += "<br/>Product = "+device.getProductId();
            outStr += "<br/>Class = "+device.getDeviceClass();
            outStr += "<br/>Subclass = "+device.getDeviceSubclass();
            outStr += "<br/>Readable = "+reader.isSupported(device);
        }
        
        callbackContext.success(outStr);
    }

    private void getUSBPermission(CallbackContext callbackContext){
        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        cordova.getActivity().registerReceiver(broadcastReceiver, filter);
        
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        callback = callbackContext;
    }

    //private static final String ACTION_USB_PERMISSION = "com.android.otb.USB_PERMISSION";
    //private static final String ACTION_USB_PERMISSION = "com.megster.nfcid.plugin.USB_PERMISSION";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) { // TODO check on synchronized
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            reader.open(device);
                            usbDevice = device;
                            callback.success("got permission");
                        }
                    } else {
                        callback.error("Permission denied for device " + device.getDeviceName());
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                callback.error("WARNING: you need to close the reader!!!!");
            }
            else{
                callback.error("unknown error: action="+action);
            }
        }
    };
}
