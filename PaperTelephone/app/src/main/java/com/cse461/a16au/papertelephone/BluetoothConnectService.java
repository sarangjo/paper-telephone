package com.cse461.a16au.papertelephone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class does the work of setting up
 * bluetooth connections between phones after
 * initializing and making sure that phones
 * have bluetooth available and that it is turned on
 * <br/>
 * As of now, this only allows for secure connections.
 */
public class BluetoothConnectService {
    private static final String TAG = "BluetoothConnectService";

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private int mState;

    // Threads
    private BluetoothThread mAcceptThread;
    private Map<String, BluetoothThread> mConnectThreads;
    private Map<String, BluetoothThread> mConnectedThreads;

    // States
    public static final int STATE_STOPPED = 0;
    public static final int STATE_STARTED = 1;
//    public static final int STATE_CONNECTING = 2;
//    public static final int STATE_CONNECTED = 3;

    public BluetoothConnectService(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mConnectThreads = new ConcurrentHashMap<>();
        mConnectedThreads = new ConcurrentHashMap<>();
    }

    // SERVER FUNCTIONS: start() and stop()

    /**
     * Starts the connect service. If any connections exist, closes them.
     */
    public void start() {
        Log.d(TAG, "Starting service");

        // Cancel threads attempting to make or currently running a connection
        stopThreads(mConnectThreads);
        stopThreads(mConnectedThreads);

        setState(STATE_STARTED);

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    /**
     * Stops all threads.
     */
    public void stop() {
        Log.d(TAG, "Stopping all threads");

        stopThreads(mConnectThreads);
        stopThreads(mConnectedThreads);

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_STOPPED);
    }

    public synchronized int getState() {
        return mState;
    }

    public synchronized void setState(int state) {
        this.mState = state;
    }

    /**
     * Stops all threads in the given thread list.
     */
    private void stopThreads(Map<String, BluetoothThread> threads) {
        if (!threads.isEmpty()) {
            Iterator<Map.Entry<String, BluetoothThread>> iter = threads.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, BluetoothThread> entry = iter.next();
                entry.getValue().cancel();
                iter.remove();
            }
        }
    }

    // CLIENT FUNCTIONS: connect()

    /**
     * Connects to the given BluetoothDevice.
     */
    public void connect(BluetoothDevice device) {
        Log.d(TAG, "Connecting to: " + device);

//        if (mState == STATE_CONNECTING) {
//            if (mConnectThread != null) {
//                mConnectThread.cancel();
//                mConnectThread = null;
//            }
//        }
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }

        // Start outgoing thread
        BluetoothThread connectThread = new ConnectThread(device);
        mConnectThreads.put(device.getAddress(), connectThread);
        connectThread.start();

//        setState(STATE_CONNECTING);
    }

    // UNIVERSAL FUNCTIONS: connected(), connectionLost(), write()

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice remoteDevice) {
        Log.d(TAG, "Connected to: " + remoteDevice.getName() + " at " + remoteDevice.getAddress());

//        if (mConnectThread != null) {
//            mConnectThread.cancel();
//            mConnectThread = null;
//        }
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }
//        if (mAcceptThread != null) {
//            mAcceptThread.cancel();
//            mAcceptThread = null;
//        }

//        setState(STATE_CONNECTED);

        // Start the new ConnectedThread
        BluetoothThread connectedThread = new ConnectedThread(socket, remoteDevice);
        mConnectedThreads.put(remoteDevice.getAddress(), connectedThread);
        connectedThread.start();

        // Send the name back to UI
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_CONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, remoteDevice.getName());
        bundle.putString(Constants.DEVICE_ADDRESS, remoteDevice.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * When a connection is lost.
     */
    private void connectionLost() {
        // TODO provide more info on whose connection was lost
        Log.d(TAG, "Connection was lost");

        // Send toast back to UI
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Connection was lost.");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        this.start();
    }

    /**
     * Write to ConnectedThread in an unsynchronized manner
     *
     * @param out
     * @return
     */
    public boolean write(byte[] out, String address) {
        ConnectedThread r;
        synchronized (mConnectedThreads) {
//            if (mState != STATE_CONNECTED) return;
            r = (ConnectedThread) mConnectedThreads.get(address);
        }
        if (r == null) {
            return false;
        }
        return r.write(out);
    }

    // THREAD IMPLEMENTATIONS

    /**
     * A general Bluetooth thread.
     */
    private abstract class BluetoothThread extends Thread {
        abstract void cancel();
    }

    /**
     * A thread that listens for and accepts incoming connections.
     */
    private class AcceptThread extends BluetoothThread {
        private final BluetoothServerSocket mmServerSocket;

        AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(Constants.APP_NAME, Constants.APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Listen failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "[ACCEPT THREAD] Begin listening...");
            BluetoothSocket socket = null;

            while (mState != STATE_STOPPED) {
                try {
                    socket = mmServerSocket.accept();

                    // Connection accepted!
                    if (socket != null) {
                        synchronized (BluetoothConnectService.this) {
                            switch (mState) {
                                case STATE_STARTED:
//                            case STATE_CONNECTING:
                                    connected(socket, socket.getRemoteDevice());
                                    break;
                                case STATE_STOPPED:
//                            case STATE_CONNECTED:
                                    // Not ready or already connected
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        Log.e(TAG, "Could not close unwanted socket", e);
                                    }
                                    break;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Accept failed", e);
                    break;
                }
            }
            Log.d(TAG, "[ACCEPT THREAD] Done listening.");
        }

        void cancel() {
            Log.d(TAG, "Cancel accept " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing server failed", e);
            }
        }
    }

    /**
     * Attempts to create a client connection with another device.
     */
    private class ConnectThread extends BluetoothThread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(Constants.APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Unable to create socket from device", e);
            }

            mmSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "[CONNECT THREAD " + mmDevice.getAddress() + "] Attempting to connect...");
            setName("ConnectThread");

            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                // Close socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Unable to close socket during connection failure", e2);
                }
                Log.e(TAG, "Unable to connect to device");
                return;
            }

            synchronized (mConnectThreads) {
                mConnectThreads.remove(mmDevice.getAddress());
//                mConnectThread = null;
            }

            Log.d(TAG, "[CONNECT THREAD " + mmDevice.getAddress() + "] Finished connect operation");

            // Connected!
            connected(mmSocket, mmDevice);
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of socket failed", e);
            }

        }
    }

    /**
     * The main communication thread that sends and receives data on a connection.
     */
    private class ConnectedThread extends BluetoothThread {
        private final BluetoothDevice mmDevice;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            Log.d(TAG, "Create connected thread");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmDevice = device;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Unable to create socket streams", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            log("Start reading and writing");

            byte[] buffer = new byte[1024];
            int bytes;

            while (mState == STATE_STARTED) {
                try {
                    bytes = mmInStream.read(buffer);
                    log("Received " + bytes + " bytes");

                    handleRead(buffer, bytes);
                } catch (IOException e) {
                    Log.e(TAG, "Connection disconnected", e);
                    connectionLost();

                    break;
                }
            }
            log("Done listening");
        }

        private void handleRead(byte[] buffer, int bytes) {
            // ByteBuffer to wrap our input buffer
            ByteBuffer input = ByteBuffer.wrap(Arrays.copyOfRange(buffer, 0, bytes));

            // Extracts header, if any
            if (bytes > 5) {
                byte[] header = new byte[Constants.HEADER_IMAGE.length];
                input.get(header, 0, 5);

                if (Arrays.equals(header, Constants.HEADER_IMAGE)) {
                    // Hold onto the full image size for later
                    int totalImageSize = input.getInt();
                    int imageSize = totalImageSize;

                    // Set up the overall buffer
                    ByteBuffer img = ByteBuffer.allocate(imageSize);

                    // Get the actual image data
                    byte[] imagePacket = new byte[bytes - 9];
                    input.get(imagePacket);
                    img.put(imagePacket);
                    imageSize -= imagePacket.length;

                    while (imageSize > 0) {
                        try {
                            bytes = mmInStream.read(buffer);
                            log("Received image packet of " + bytes + " bytes");

                            img.put(Arrays.copyOfRange(buffer, 0, bytes));
                            imageSize -= bytes;
                        } catch (IOException e) {
                            Log.e(TAG, "Connection disconnected", e);
                            connectionLost();

                            break;
                        }
                    }

                    mHandler.obtainMessage(Constants.MESSAGE_READ, totalImageSize, Constants.MESSAGE_RECV_IMAGE, img.array())
                            .sendToTarget();

                    return;
                }
            }
            // TODO: minor, maybe consider truncating, not a big deal
            mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.MESSAGE_RECV_TEXT, buffer)
                    .sendToTarget();
        }

        public boolean write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
            return false;
        }

        void cancel() {
            // TODO: implement
        }

        private void log(String str) {
            Log.d(TAG, "[CONNECTED THREAD " + mmDevice.getAddress() + "] " + str);
        }
    }

}
