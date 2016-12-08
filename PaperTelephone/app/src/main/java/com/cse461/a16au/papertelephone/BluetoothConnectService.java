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
 * This singleton class does the work of setting up
 * bluetooth connections between phones after
 * initializing and making sure that phones
 * have bluetooth available and that it is turned on
 * <br/>
 * As of now, this only allows for secure connections.
 */
public class BluetoothConnectService {
    private static final String TAG = "BluetoothConnectService";

    private final BluetoothAdapter mAdapter;

    private Handler mMainHandler;
    private Handler mGameHandler;
    private int mState;

    // Threads
    private BluetoothThread mAcceptThread;
    private final Map<String, BluetoothThread> mConnectThreads;
    private final Map<String, BluetoothThread> mConnectedThreads;

    // States
    public static final int STATE_STOPPED = 0;
    public static final int STATE_STARTED = 1;

    private static BluetoothConnectService ourInstance = new BluetoothConnectService();

    private BluetoothConnectService() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mConnectThreads = new ConcurrentHashMap<>();
        mConnectedThreads = new ConcurrentHashMap<>();
    }

    public static BluetoothConnectService getInstance() {
        return ourInstance;
    }

    public void registerGameHandler(Handler handler) {
        mGameHandler = handler;
    }

    public void unregisterGameHandler(Handler gameHandler) {
        mGameHandler = (mGameHandler.equals(gameHandler) ? null : mGameHandler);
    }

    public void registerMainHandler(Handler handler) {
        mMainHandler = handler;
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
    public void connect(String address) {
        BluetoothDevice device = mAdapter.getRemoteDevice(address);

        Log.d(TAG, "Connecting to: " + device);

        // If we're already discovering, stop it
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }

        // Start outgoing thread
        BluetoothThread connectThread = new ConnectThread(device);
        mConnectThreads.put(device.getAddress(), connectThread);
        connectThread.start();
    }

    // UNIVERSAL FUNCTIONS: connected(), connectionLost(), write()

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice remoteDevice) {
        Log.d(TAG, "Connected to: " + remoteDevice.getName() + " at " + remoteDevice.getAddress());

        // Start the new ConnectedThread
        BluetoothThread connectedThread = new ConnectedThread(socket, remoteDevice);
        mConnectedThreads.put(remoteDevice.getAddress(), connectedThread);
        connectedThread.start();

        // Send the name back to UI
        Message msg = mMainHandler.obtainMessage(Constants.MESSAGE_CONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, remoteDevice.getName());
        bundle.putString(Constants.DEVICE_ADDRESS, remoteDevice.getAddress());
        msg.setData(bundle);
        mMainHandler.sendMessage(msg);
    }

    /**
     * When a connection is lost.
     */
    private void connectionLost(BluetoothDevice device) {
        Log.d(TAG, "Connection was lost");

        // Send toast back to UI
        Message msg = mMainHandler.obtainMessage(Constants.MESSAGE_DISCONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_ADDRESS, device.getAddress());
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mMainHandler.sendMessage(msg);
    }

    /**
     * Write to ConnectedThread synchronously.
     *
     * @return success
     */
    public boolean write(byte[] out, String address) {
        ConnectedThread thread;

//            if (mState != STATE_CONNECTED) return;
        thread = (ConnectedThread) mConnectedThreads.get(address);

        return thread != null && thread.write(out);
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
                Log.e(TAG, "Unable to connect to device", e);
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
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
            Log.d(TAG, "Create connected thread");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmDevice = device;
            mmSocket = socket;

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
                    connectionLost(mmDevice);

                    break;
                }
            }
            log("Done listening");
        }

        private void handleRead(byte[] data, int bytes) {
            data = Arrays.copyOfRange(data, 0, bytes);
            // ByteBuffer to wrap our input buffer
            ByteBuffer input = ByteBuffer.wrap(data);

            // Message to be passed to the appropriate handler
            Message msg;
            Handler currHandler = mMainHandler;

            // Extracts header, if any
            if (bytes >= Constants.HEADER_LENGTH) {
                byte[] header = new byte[Constants.HEADER_LENGTH];
                input.get(header);

                // Game handler
                if (Arrays.equals(header, Constants.HEADER_IMAGE)) {
                    // Hold onto the full image size for later
                    int totalImageSize = input.getInt();
                    int imageSize = totalImageSize;
                    log("Image Size is " + imageSize);

                    // Set up the overall buffer
                    ByteBuffer img = ByteBuffer.allocate(imageSize);

                    // Get the actual image data
                    byte[] imagePacket = new byte[bytes - 4];
                    input.get(imagePacket);
                    img.put(imagePacket);
                    imageSize -= imagePacket.length;

                    while (imageSize > 0) {
                        try {
                            bytes = mmInStream.read(data);
                            log("Received image packet of " + bytes + " bytes");

                            log(imageSize + " bytes left to store image");
                            img.put(Arrays.copyOfRange(data, 0, bytes));
                            imageSize -= bytes;
                        } catch (IOException e) {
                            Log.e(TAG, "Connection disconnected", e);
                            connectionLost(mmDevice);

                            break;
                        }
                    }

                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, totalImageSize, Constants.READ_IMAGE, img.array());
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_PROMPT)) {
                    data = new byte[bytes - Constants.HEADER_LENGTH];
                    input.get(data);
                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_PROMPT, data);
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_DONE)) {
                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_DONE, data);
                    currHandler = mGameHandler;
                }
                // Main Handler
                else if (Arrays.equals(header, Constants.HEADER_START)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_START, data);
                } else if (Arrays.equals(header, Constants.HEADER_SUCCESSOR)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_SUCCESSOR, data);
                } else if (Arrays.equals(header, Constants.HEADER_DEVICES)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_DEVICES, data);
                } else if (Arrays.equals(header, Constants.HEADER_PING)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_PING, data);
                } else {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_UNKNOWN, data);
                }
            } else {
                msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_UNKNOWN, data);
            }

            if (currHandler != null) {
                Bundle bundle = new Bundle();
                bundle.putString(Constants.DEVICE_ADDRESS, mmDevice.getAddress());
                bundle.putString(Constants.DEVICE_NAME, mmDevice.getName());
                msg.setData(bundle);
                currHandler.sendMessage(msg);
            }
        }

        public boolean write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Send the name back to UI
                Message msg = mMainHandler.obtainMessage(Constants.MESSAGE_WRITE);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.DEVICE_NAME, mmDevice.getName());
                bundle.putString(Constants.DEVICE_ADDRESS, mmDevice.getAddress());
                msg.setData(bundle);
                return mMainHandler.sendMessage(msg);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
            return false;
        }

        void cancel() {
            // TODO: implement
            try {
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to cancel connected thread", e);
            }

        }

        private void log(String str) {
            Log.d(TAG, "[CONNECTED THREAD " + mmDevice.getAddress() + "] " + str);
        }
    }

}
