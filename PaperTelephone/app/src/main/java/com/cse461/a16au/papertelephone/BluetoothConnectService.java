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
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;

    // States
    public static final int STATE_STOPPED = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public BluetoothConnectService(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }

    // SERVER FUNCTIONS: start() and stop()

    public void start() {
        Log.d(TAG, "start");

        // Cancel threads attempting to make or currently running a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

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
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
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

    // CLIENT FUNCTIONS: connect(), connectFailed()

    public void connect(BluetoothDevice device) {
        Log.d(TAG, "Connecting to: " + device);

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start outgoing thread
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        setState(STATE_CONNECTING);
    }

    /**
     * Connection to a specific remote device failed.
     */
    private void connectFailed() {
        Log.e(TAG, "Unable to connect to device");

        this.start();
    }

    // UNIVERSAL FUNCTIONS: connected(), connectionLost(), write()

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice remoteDevice) {
        Log.d(TAG, "Connected!");

        // Cancel any existing threads
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // TODO: we should allow AcceptThreads
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the new ConnectedThread
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name back to UI
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_CONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, remoteDevice.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * When a connection is lost.
     */
    private void connectionLost() {
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
     */
    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    // THREAD IMPLEMENTATIONS

    /**
     * A thread that listens for and accepts incoming connections.
     */
    private class AcceptThread extends Thread {
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
            Log.d(TAG, "Begin listening...");
            BluetoothSocket socket = null;

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Accept failed", e);
                    break;
                }

                // Connection accepted!
                if (socket != null) {
                    synchronized (BluetoothConnectService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_STOPPED:
                            case STATE_CONNECTED:
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
            }
            Log.d(TAG, "Done listening.");
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
    private class ConnectThread extends Thread {
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
            Log.d(TAG, "Attempting to connect...");
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
                connectFailed();
                return;
            }

            synchronized (BluetoothConnectService.this) {
                mConnectThread = null;
            }

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
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "Create connected thread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

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
            Log.d(TAG, "Start reading and writing");

            byte[] buffer = new byte[1024];
            int bytes;

            while (mState == STATE_CONNECTED) {
                try {
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Connection disconnected", e);
                    connectionLost();

                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {

        }
    }

}
