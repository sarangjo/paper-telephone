package com.cse461.a16au.papertelephone.game;

import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO: class documentation
 */

public class GameData {
    // SETUP
    /**
     * List of all connected device address
     */
    public static List<String> connectedDevices = new ArrayList<>();

    /**
     * List of all connected device names
     */
    public static List<String> connectedDeviceNames = new ArrayList<>();

    /**
     * Index of this device's successor in the list of connectedDevices
     */
    public static int nextDevice = 0;

    /**
     * Index of the device that started the game, -1 if it is this device
     */
    public static int startDevice = -1;

    /**
     * Keeps track of how many devices have been chosen so far in that start game process
     */
    public static int lastSuccessor = 0;

    /**
     * Set of devices that have not been chosen as a successor in the start game process
     */
    public static Set<String> unplacedDevices = new HashSet<>();

    /**
     * Keeps track of how many turns remain until the end of the game
     */
    public static int turnsLeft = 0;

    /**
     * Map from device addresses to their "Summaries" which store the original
     * prompt, along with the drawings and prompts that followed it
     */
    public static Map<String, List<Byte[]>> addressToSummaries = new HashMap<>();

    /**
     * This device's mac address
     */
    public static String mAddress = null;

    // IN-GAME
    public static CountDownTimer turnTimer = null;
    /**
     * A list of the devices that have not finished the current turn.
     */
    public static List<String> unfinishedDeviceList = new ArrayList<>();

    /**
     * Stores whether or not this device has completed the current turn
     */
    public static boolean isDone;
}
