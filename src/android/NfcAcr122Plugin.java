package com.emisys.cordova.nfc;

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

import java.util.Map;

public class NfcAcr122Plugin extends CordovaPlugin {

    //private static final String TAG = "NfcIdPlugin";

    private static final String STARTLISTEN = "startListen";
    private static final String STOPLISTEN = "stopListen";

    private static final String[] stateStrings = {"Unknown", "Absent",
        "Present", "Swallowed", "Powered", "Negotiable", "Specific"};

    private UsbManager usbManager;
    private UsbDevice usbDevice;

    private Reader reader;
    private PendingIntent mPermissionIntent;
    
    private CallbackContext callback;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    //private static final String ACTION_USB_PERMISSION = "com.megster.nfcid.plugin.USB_PERMISSION";
    
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
                        }

                    } else {

                        //Log.d(TAG, "Permission denied for device " + device.getDeviceName());

                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                //Log.w(TAG, "WARNING: you need to close the reader!!!!");

            }
        }
    };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        //Log.d(TAG, "execute " + action);
        if (action.equalsIgnoreCase(STARTLISTEN)) {
            listen(callbackContext);
        } else if (action.equalsIgnoreCase(STOPLISTEN)) {
            startNfc();
        } else {
            // invalid action
            return false;
        }
        return true;
    }
	
    private void startListen(CallbackContext callbackContext) {
    	// Get USB manager
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Initialize reader
        reader = new Reader(usbManager);
        reader.setOnStateChangeListener(new OnStateChangeListener() {

            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {

                if (prevState < Reader.CARD_UNKNOWN
                        || prevState > Reader.CARD_SPECIFIC) {
                    prevState = Reader.CARD_UNKNOWN;
                }

                if (currState < Reader.CARD_UNKNOWN
                        || currState > Reader.CARD_SPECIFIC) {
                    currState = Reader.CARD_UNKNOWN;
                }

                // Create output string
                final String outputString = "Slot " + slotNum + ": "
                        + stateStrings[prevState] + " -> "
                        + stateStrings[currState];

                // Show output
                //this.webView.sendJavascript("tag");
            }
        });

        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);
		
		//setup listening
		Map<String, UsbDevice> devices = usbManager.getDeviceList();
        UsbDevice device = devices.values().toArray(new UsbDevice[0])[0];
        usbManager.requestPermission(device, mPermissionIntent);

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(result);
		
		callbackContext.success();
    }

    
    private void endListen(CallbackContext callbackContext){
		
		
		callbackContext.success();
	}
    
    private void startNfc() {
        //Log.d(TAG, "startNfc");
        if (usbDevice != null) {
            reader.open(usbDevice);
        }
    }

    private void stopNfc() {
        //Log.d(TAG, "stopNfc");
        if (usbDevice != null) {
            reader.close();
        }
    }
	
    @Override
    public void onPause(boolean multitasking) {
        //Log.d(TAG, "onPause " + getIntent());
        super.onPause(multitasking);
        if (multitasking) {
            // nfc can't run in background
            stopNfc();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        //Log.d(TAG, "onResume " + getIntent());
        super.onResume(multitasking);
        startNfc();
    }
	/*
    private Activity getActivity() {
        return this.cordova.getActivity();
    }

    private Intent getIntent() {
        return getActivity().getIntent();
    }
	*/
}
