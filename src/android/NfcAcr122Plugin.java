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

public class NfcAcr122Plugin extends CordovaPlugin  {

    private static final String TAG = "NfcAcr122Plugin";

    private static final String LISTEN = "startListen";

    private UsbManager usbManager;
    private UsbDevice usbDevice;

    private Reader reader;
    PendingIntent mPermissionIntent;

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

        //                Log.d(TAG, "Permission denied for device " + device.getDeviceName());

                    }
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

        //        Log.w(TAG, "WARNING: you need to close the reader!!!!");

            }
        }
    };


    private CallbackContext callback;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // your init code here

    //    Log.d(TAG, "initializing...");

        // Get USB manager
        usbManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);

        // Initialize reader
        reader = new Reader(usbManager);
        reader.setOnStateChangeListener(new Reader.OnStateChangeListener() {

            @Override
            public void onStateChange(int slotNumber, int previousState, int currentState) {
/*
                Log.d(TAG, "slotNumber " + slotNumber);
                Log.d(TAG, "previousState " + previousState);
                Log.d(TAG, "currentState " + currentState);
                Log.d(TAG, "");
*/
                if (currentState == Reader.CARD_PRESENT) {
                    Log.d(TAG, "Ready to read!!!!");

                    // TODO refactor logic to getUidForConnectedCard
                    //byte[] sendBuffer = new byte[]{ (byte)0xFF, (byte)0xCA, (byte)0x0, (byte)0x0, (byte)0x4} ;
                    // length of 0 gets the whole ID!
                    byte[] sendBuffer = new byte[]{ (byte)0xFF, (byte)0xCA, (byte)0x0, (byte)0x0, (byte)0x0};
                    byte[] receiveBuffer = new byte[16];

                    try {
                        int byteCount = reader.control(slotNumber, Reader.IOCTL_CCID_ESCAPE, sendBuffer, sendBuffer.length, receiveBuffer, receiveBuffer.length);

                        // TODO errors should have byteCount of 2
                        // TODO send some bad commands and check for the codes from the spec

                        Log.w(TAG, "====================");
                        for (byte b : receiveBuffer) {
                            Log.w(TAG, "byte " + b);
                        }
                        Log.w(TAG, "====================");

                        //int MIFARE_CLASSIC_UID_LENGTH = 4;
                        StringBuffer uid = new StringBuffer();
                        for (int i = 0; i < (byteCount - 2); i++) {

                            uid.append(String.format("%02X", receiveBuffer[i]));
                            if (i < byteCount - 3) {
                                uid.append(":");
                            }
                            Log.w(TAG, String.format("%02X ", receiveBuffer[i]));
                        }

                        // TODO plugin should just return the UID as byte[]
                        Log.w(TAG, uid.toString());

                        PluginResult result = new PluginResult(PluginResult.Status.OK, uid.toString());
                        result.setKeepCallback(true);
                        callback.sendPluginResult(result);

                    } catch (ReaderException e) {
                        e.printStackTrace();
                    }

                } else if (currentState == Reader.CARD_ABSENT && previousState == Reader.CARD_PRESENT) {
                    // this is probably OK,
                    // we'll want to do something for card lost if we were in the middle of reading
                    Log.d(TAG, "Card Lost");
                }


            }
        });

        /// -----

        // Register receiver for USB permission
        mPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        getActivity().registerReceiver(broadcastReceiver, filter);

    }


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

    //    Log.d(TAG, "execute " + action);

        // TODO call error callback if there is no reader

        if (action.equalsIgnoreCase(LISTEN)) {
            listen(callbackContext);
        } else {
            // invalid action
            return false;
        }

        return true;
    }

    private void listen(CallbackContext callbackContext) {

        Map<String, UsbDevice> devices = usbManager.getDeviceList();
        UsbDevice device = devices.values().toArray(new UsbDevice[0])[0];
        usbManager.requestPermission(device, mPermissionIntent);

        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(result);
    }

    private void startNfc() {
   //     Log.d(TAG, "startNfc");

    }

    private void stopNfc() {
        //Log.d(TAG, "stopNfc");
        
    }


    @Override
    public void onPause(boolean multitasking) {
        Log.d(TAG, "onPause " + getIntent());
        super.onPause(multitasking);
        if (multitasking) {
            // nfc can't run in background
            stopNfc();            
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "onResume " + getIntent());
        super.onResume(multitasking);
        startNfc();
    }

    private Activity getActivity() {
        return this.cordova.getActivity();
    }

    private Intent getIntent() {
        return getActivity().getIntent();
    }

}
