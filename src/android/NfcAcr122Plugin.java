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

public class NfcAcr122Plugin extends CordovaPlugin  {
    private static final String LISTEN = "startListen";

    private UsbManager usbManager;
    private UsbDevice usbDevice;

    private Reader reader;
    PendingIntent mPermissionIntent;
    
    private CallbackContext callbackContext = null;

    private static final String ACTION_USB_PERMISSION = "com.android.otb.USB_PERMISSION";
    //private static final String ACTION_USB_PERMISSION = "com.megster.nfcid.plugin.USB_PERMISSION";


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        
        if (action.equalsIgnoreCase(LISTEN)) {
            test(callbackContext);
        } else {
            // invalid action
            return false;
        }

        return true;
    }

    private void test(CallbackContext callbackContext) {
        callbackContext.success("plugin works");
    }

}
