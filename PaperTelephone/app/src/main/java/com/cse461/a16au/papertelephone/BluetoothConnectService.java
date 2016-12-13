package com.cse461.a16au.papertelephone;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
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

//    private CountDownTimer mConnectTimer;

    /**
     * Connects to the given BluetoothDevice.
     */
    public void connect(String address) {
        final BluetoothDevice device = mAdapter.getRemoteDevice(address);

        Log.d(TAG, "Connecting to: " + device);

        // If we're already discovering, stop it
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }

        // Start outgoing thread
        BluetoothThread connectThread = new ConnectThread(device);
        mConnectThreads.put(device.getAddress(), connectThread);
        connectThread.start();

        // Set timeout for connect thread
//        mConnectTimer = new CountDownTimer(Constants.TIMEOUT_MILLIS, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//            }
//
//            @Override
//            public void onFinish() {
//                BluetoothThread thread;
//                synchronized (BluetoothConnectService.this) {
//                    thread = mConnectThreads.get(device.getAddress());
//                }
//                if (thread != null) {
//                    thread.cancel();
//                }
//
//                // Send error message back to UI
//                Message msg = mMainHandler.obtainMessage(Constants.MESSAGE_CONNECT_FAILED);
//                Bundle bundle = new Bundle();
//                bundle.putString(Constants.DEVICE_ADDRESS, device.getAddress());
//                bundle.putString(Constants.DEVICE_NAME, device.getName());
//                msg.setData(bundle);
//                mMainHandler.sendMessage(msg);
//            }
//        }.start();
    }

    // UNIVERSAL FUNCTIONS: connected(), connectionLost(), write()

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice remoteDevice) {
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
     * Write to ConnectedThread synchronously. Can potentially modify the given array.
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
                // TODO: timeout for this
                mmSocket.connect();
            } catch (IOException e) {
                // Close socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Unable to close socket during connection failure", e2);
                }
                Log.e(TAG, "Unable to connect to device", e);

                // Send error message back to UI
                Message msg = mMainHandler.obtainMessage(Constants.MESSAGE_CONNECT_FAILED);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.DEVICE_ADDRESS, mmDevice.getAddress());
                bundle.putString(Constants.DEVICE_NAME, mmDevice.getName());
                msg.setData(bundle);
                mMainHandler.sendMessage(msg);

                return;
            }

            synchronized (this) {
//                mConnectTimer.cancel();
                mConnectThreads.remove(mmDevice.getAddress());
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
//            Log.d(TAG, "Create connected thread");
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
//                    log("Received " + bytes + " bytes");

                    handleRead(buffer, bytes);
                } catch (IOException e) {
                    Log.e(TAG, "Connection disconnected", e);
                    connectionLost(mmDevice);

                    break;
                }
            }
            log("Done listening");
        }

        private void handleRead(byte[] input, int bytes) {
            input = Arrays.copyOfRange(input, 0, bytes);

            // Message to be passed to the appropriate handler
            Message msg;
            Bundle bundle = new Bundle();
            Handler currHandler = mMainHandler;
            byte[] creatorAddressArr = new byte[Constants.ADDRESS_LENGTH];

            // Extracts header, if any
            if (bytes >= Constants.HEADER_LENGTH) {
                byte[] header = Arrays.copyOf(input, Constants.HEADER_LENGTH);
                log("Received: " + new String(header));

                // ByteBuffer to wrap our input array
                ByteBuffer dataBuffer = ByteBuffer.wrap(Arrays.copyOfRange(input, Constants.HEADER_LENGTH, bytes));

                // Game handler
                if (Arrays.equals(header, Constants.HEADER_IMAGE)) {
                    // Get Creator Address
                    dataBuffer.get(creatorAddressArr);
                    bundle.putString(Constants.CREATOR_ADDRESS, new String(creatorAddressArr));

                    byte[] imgData = processImage(dataBuffer, bytes - Constants.ADDRESS_LENGTH - Constants.HEADER_LENGTH - 4, input);

                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, imgData.length, Constants.READ_IMAGE, imgData);
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_PROMPT)) {
                    // Get Creator Address
                    dataBuffer.get(creatorAddressArr);
                    bundle.putString(Constants.CREATOR_ADDRESS, new String(creatorAddressArr));

                    input = new byte[bytes - Constants.ADDRESS_LENGTH - Constants.HEADER_LENGTH];
                    dataBuffer.get(input);
                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_PROMPT, input);
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_DONE)) {
                    // Get the type of data that this DONE packet contains
                    dataBuffer.get(header);

                    dataBuffer.get(creatorAddressArr);
                    bundle.putString(Constants.CREATOR_ADDRESS, new String(creatorAddressArr));

                    log("Done Header:" + new String(header));

                    // Determine which type of DONE message we received
                    byte[] doneData;

                    if (Arrays.equals(header, Constants.HEADER_IMAGE)) {
                        doneData = processImage(dataBuffer, bytes - 2 * Constants.HEADER_LENGTH - Constants.ADDRESS_LENGTH - 4, input);
                    } else {
                        doneData = new byte[bytes - 2 * Constants.HEADER_LENGTH - Constants.ADDRESS_LENGTH];
                        dataBuffer.get(doneData);
                    }

                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_DONE, doneData);
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_REQUEST_SUCCESSOR)) {
                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_REQUEST_SUCCESSOR, dataBuffer.array());
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_RESPONSE_SUCCESSOR)) {
                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_RESPONSE_SUCCESSOR, dataBuffer.array());
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_NEW_START)) {
                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_NEW_START, dataBuffer.array());
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_DTG)) {
                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_DTG, dataBuffer.array());
                    currHandler = mGameHandler;
                } else if (Arrays.equals(header, Constants.HEADER_GIVE_SUCCESSOR)) {
                    msg = mGameHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_GIVE_SUCCESSOR, dataBuffer.array());
                    currHandler = mGameHandler;
                // Main Handler
                } else if(Arrays.equals(header, Constants.HEADER_RETURN_TO_LOBBY)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_RTL, dataBuffer.array());
                }
                else if (Arrays.equals(header, Constants.HEADER_START)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_START, dataBuffer.array());
                } else if (Arrays.equals(header, Constants.HEADER_START_ACK)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_START_ACK, dataBuffer.array());
                } else if (Arrays.equals(header, Constants.HEADER_SUCCESSOR)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_SUCCESSOR, dataBuffer.array());
                } else if (Arrays.equals(header, Constants.HEADER_DEVICES)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_DEVICES, dataBuffer.array());
                } else if (Arrays.equals(header, Constants.HEADER_PING)) {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_PING, dataBuffer.array());
                } else {
                    msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_UNKNOWN, dataBuffer.array());
                }
            } else {
                msg = mMainHandler.obtainMessage(Constants.MESSAGE_READ, bytes, Constants.READ_UNKNOWN, input);
            }

            if (currHandler != null) {
                bundle.putString(Constants.DEVICE_ADDRESS, mmDevice.getAddress());
                bundle.putString(Constants.DEVICE_NAME, mmDevice.getName());
                msg.setData(bundle);
                currHandler.sendMessage(msg);
            }
        }

        public byte[] processImage(ByteBuffer input, int imgBytes, byte[] data) {
            // Hold onto the full image size for later
            int totalImageSize = input.getInt();
            int remainingImageSize = totalImageSize;

            log("Receiving image of size " + remainingImageSize);

            // Set up the overall buffer
            ByteBuffer imgBuffer = ByteBuffer.allocate(remainingImageSize);

            // Get the actual image data
            byte[] imagePacket = new byte[imgBytes];
            input.get(imagePacket);
            imgBuffer.put(imagePacket);
            remainingImageSize -= imagePacket.length;

            int bytesRead = 0;

            while (remainingImageSize > 0) {
                try {
                    bytesRead = mmInStream.read(data);
                    imgBuffer.put(Arrays.copyOfRange(data, 0, bytesRead));
                    remainingImageSize -= bytesRead;
                } catch (IOException e) {
                    Log.e(TAG, "Connection disconnected", e);
                    connectionLost(mmDevice);

                    break;
                }
            }
            log(remainingImageSize + " bytes left to store image");

            return imgBuffer.array();
        }

        public boolean write(byte[] buffer) {
            try {
                if (buffer.length >= Constants.HEADER_LENGTH)
                    log("Send: " + new String(Arrays.copyOfRange(buffer, 0, Constants.HEADER_LENGTH)));
                else
                    log("Send: " + new String(buffer));

                mmOutStream.write(buffer);
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
            Log.d(TAG, "[CONNECTED THREAD " + mmDevice.getName() + "] " + str);
        }
    }
}
