package com.cse461.a16au.papertelephone;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by saran on 11/24/2016. TODO
 */
public class Constants {
    public static final UUID APP_UUID = UUID.fromString("b914c3a4-e47f-4fa8-b23a-8e55a5981e5f");
    public static final String APP_NAME = "PaperTelephone";

    // Activity intent requests
    public static final int REQUEST_ENABLE_BT = 1;

    // Message sent from BluetoothConnectService to any Handlers
    public static final int MESSAGE_CONNECTED = 0;
    public static final int MESSAGE_DISCONNECTED = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;

    // Bundle keys
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";

    // Bluetooth packet system
    public static final int HEADER_LENGTH = 12;

    public static final int READ_UNKNOWN = -1;
    public static final byte[] HEADER_START = getHeader("START");
    public static final int READ_START = 0;
    public static final byte[] HEADER_IMAGE = getHeader("IMAGE");
    public static final int READ_IMAGE = 1;
    public static final byte[] HEADER_PROMPT = getHeader("PROMPT");
    public static final int READ_PROMPT = 2;
    public static final byte[] HEADER_PING = getHeader("PING");
    public static final int READ_PING = 3;
    public static final byte[] HEADER_DEVICES = getHeader("DEVICES");
    public static final int READ_DEVICES = 4;
    public static final byte[] HEADER_SUCCESSOR = getHeader("SUCCESSOR");
    public static final int READ_SUCCESSOR = 5;
    public static final byte[] HEADER_DONE = getHeader("DONE");
    public static final int READ_DONE = 6;

    public static final int ADDRESS_LENGTH = 17;

    /**
     * Duration of discoverability at a given time, in seconds.
     */
    public static final int DISCOVERABLE_TIME = 300;

    /**
     * Duration of a single turn, in milliseconds.
     */
    public static final long TURN_MILLIS = 30000;

    /**
     * Creates a zero-padded header of the given string that is HEADER_LENGTH bytes long.
     */
    private static byte[] getHeader(String s) {
        return Arrays.copyOf(s.getBytes(), HEADER_LENGTH);
    }
}
