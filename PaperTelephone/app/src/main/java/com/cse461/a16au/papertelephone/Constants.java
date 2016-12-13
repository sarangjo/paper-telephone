package com.cse461.a16au.papertelephone;

import java.util.Arrays;
import java.util.UUID;

/**
 * Created by saran on 11/24/2016. TODO
 */
public class Constants {
    public static final UUID APP_UUID = UUID.fromString("b914c3a4-e47f-4fa8-b23a-8e55a5981e5f");
    public static final String APP_NAME = "PaperTelephone";

    public static final String WE_ARE_START = "START";
    public static final String NO_START = "NOSTART";

    // Activity intent requests
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_PLAY_GAME = 2;
    public static final int REQUEST_END_GAME = 3;

    // End game results
    public static final int RESULT_LOBBY = 1;
    public static final int RESULT_RESTART = 2;

    // Message sent from BluetoothConnectService to any Handlers
    public static final int MESSAGE_CONNECTED = 0;
    public static final int MESSAGE_DISCONNECTED = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_CONNECT_FAILED = 3;

    // Bundle keys
    public static final String CREATOR_ADDRESS = "creator_address";
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
    public static final byte[] HEADER_START_ACK = getHeader("ACK");
    public static final int READ_START_ACK = 7;
    public static final byte[] HEADER_REQUEST_SUCCESSOR = getHeader("REQSUCC");
    public static final int READ_REQUEST_SUCCESSOR = 8;
    public static final byte[] HEADER_RESPONSE_SUCCESSOR = getHeader("RESSUCC");
    public static final int READ_RESPONSE_SUCCESSOR = 9;
    public static final byte[] HEADER_NEW_START = getHeader("NEWSTART");
    public static final int READ_NEW_START = 10;
    public static final byte[] HEADER_GIVE_SUCCESSOR = getHeader("GIVESUCC");
    public static final int READ_GIVE_SUCCESSOR = 11;
    public static final byte[] HEADER_DTG = getHeader("DTG");
    public static final int READ_DTG = 12;

    public static final int ADDRESS_LENGTH = 17;

    /**
     * Duration of discoverability at a given time, in seconds.
     */
    public static final int DISCOVERABLE_TIME = 300;

    /**
     * Duration of a single turn, in milliseconds.
     */
    public static final long TURN_MILLIS = 30000;


    public static final long TIMEOUT_MILLIS = 10000;

    /**
     * TODO: change back to 2
     */
    public static final int MIN_PLAYERS = 3;
    public static final String JOIN_MID_GAME = "join_mid_game";

    /**
     * Creates a zero-padded header of the given string that is HEADER_LENGTH bytes long.
     */
    private static byte[] getHeader(String s) {
        return Arrays.copyOf(s.getBytes(), HEADER_LENGTH);
    }
}
