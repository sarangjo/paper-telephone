package com.cse461.a16au.papertelephone.game;

import android.bluetooth.BluetoothAdapter;
import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * TODO: class documentation
 */

public class GameData {

    public static final int NO_START = -2;
    public static final int WE_ARE_START = -1;

    private static final GameData ourInstance = new GameData();

    /**
     * Index of the device that started the game, -1 if it is this device
     */
    private int startDevice;

    /**
     * List of all connected device address
     */
    private List<String> connectedDevices;

    /**
     * List of all connected device names
     */
    private List<String> connectedDeviceNames;

    /**
     * Devices that have not acked our start yet.
     */
    private Set<String> unackedDevices;

    private GameData() {
        this.startDevice = NO_START;
        this.connectedDevices = new ArrayList<>();
        this.connectedDeviceNames = new ArrayList<>();
        this.unackedDevices = new HashSet<>();
    }

    public static GameData getInstance() {
        return ourInstance;
    }

    public synchronized int getStartDevice() {
        return startDevice;
    }

    public synchronized void setStartDevice(int startDevice) {
        this.startDevice = startDevice;
    }

    public void setStartDevice(String startDeviceAddress) {
        this.setStartDevice(getConnectedDevices().indexOf(startDeviceAddress));
    }

    public List<String> getConnectedDevices() {
        return Collections.unmodifiableList(connectedDevices);
    }

    public String getConnectedDevice(int index) {
        return getConnectedDevices().get(index);
    }

    public List<String> getConnectedDeviceNames() {
        return Collections.unmodifiableList(connectedDeviceNames);
    }

    public void addConnectedDevice(String address, String name) {
        synchronized (this) {
            this.connectedDevices.add(address);
            this.connectedDeviceNames.add(name);
        }
    }

    public void removeConnectedDevice(String address, String name) {
        synchronized (this) {
            this.connectedDevices.remove(address);
            this.connectedDeviceNames.remove(name);
        }
    }

    public void setupUnackedDevices() {
        unackedDevices.addAll(getConnectedDevices());
    }

    public void removeUnackedDevice(String deviceAddress) {
        synchronized (this) {
            unackedDevices.remove(deviceAddress);
        }
    }

    public boolean doneAcking() {
        synchronized (this) {
            return this.unackedDevices.isEmpty();
        }
    }

    // SETUP

    /**
     * Index of this device's successor in the list of connectedDevices
     */
    public static int nextDevice = 0;

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
     * This device's mac address
     */
    public static String mLocalAddress = null;

    /**
     * Our name.
     */
    public static String mName = BluetoothAdapter.getDefaultAdapter().getName();

    // IN-GAME
    /**
     * The timer for each turn.
     */
    public static CountDownTimer turnTimer = null;

    /**
     * A list of the devices that have not finished the current turn.
     */
    public static Set<String> unfinishedDeviceList = new ConcurrentSkipListSet<>();

    /**
     * Stores whether or not this device has completed the current turn
     */
    public static boolean isDone;

    public static ConnectionChangeListener connectionChangeListener;

    // END-GAME
    /**
     * Map from device addresses to their "Summaries" which store the original
     * prompt, along with the drawings and prompts that followed it
     */
    public static ConcurrentMap<String, List<byte[]>> addressToSummaries;

    public static void saveData(String creatorAddress, byte[] data) {
        List<byte[]> summary;
        if (addressToSummaries.containsKey(creatorAddress)) {
            summary = addressToSummaries.get(creatorAddress);
        } else {
            summary = new ArrayList<byte[]>();
        }
        summary.add(data);
        addressToSummaries.put(creatorAddress, summary);
    }
}
