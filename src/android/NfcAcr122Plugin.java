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
    private static final String LISTEN = "startListen";

    private UsbManager usbManager;
    private UsbDevice usbDevice;

    private Reader reader;
    private PendingIntent mPermissionIntent;
    
    private CallbackContext callbackContext = null;

    private static final String ACTION_USB_PERMISSION = "com.android.otb.USB_PERMISSION";
    //private static final String ACTION_USB_PERMISSION = "com.megster.nfcid.plugin.USB_PERMISSION";


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        
        if (action.equalsIgnoreCase(LISTEN)) {
            listen(callbackContext);
        }
        else if(action.equalsIgnoreCase("test")){
            test(callbackContext);
        }
        else if(action.equalsIgnoreCase("getUSBDevices")){
            getUSBDevices(callbackContext);
        }
        else {
            // invalid action
            return false;
        }

        return true;
    }
    
    private void listen(CallbackContext callbackContext){
        
    }

    private void test(CallbackContext callbackContext) {
        callbackContext.success("plugin works");
    }
    
    private void getUSBDevices(CallbackContext callbackContext){
        // Get USB manager
        usbManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);

        // Initialize reader
        reader = new Reader(usbManager);
        
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

}
