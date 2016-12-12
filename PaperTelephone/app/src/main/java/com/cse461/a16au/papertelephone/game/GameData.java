package com.cse461.a16au.papertelephone.game;

import android.bluetooth.BluetoothAdapter;
import android.os.CountDownTimer;

import com.cse461.a16au.papertelephone.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * TODO: class documentation
 */

public class GameData {

    private static final GameData ourInstance = new GameData();

    // The start device

    /**
     * Index of the device that started the game, -1 if it is this device
     */
    private String startDevice;

    public synchronized String getStartDevice() {
        return startDevice;
    }

    public synchronized void setStartDevice(String startDevice) {
        this.startDevice = startDevice;
    }

    // Devices we are currently connected to

    /**
     * List of all connected device address
     */
    private List<String> connectedDevices;

    /**
     * List of all connected device names
     */
    private List<String> connectedDeviceNames;

    public List<String> getConnectedDevices() {
        return Collections.unmodifiableList(connectedDevices);
    }

    public List<String> getConnectedDeviceNames() {
        return Collections.unmodifiableList(connectedDeviceNames);
    }

    public synchronized void addConnectedDevice(String address, String name) {
        this.connectedDevices.add(address);
        this.connectedDeviceNames.add(name);
    }

    public synchronized void removeConnectedDevice(String address, String name) {
        this.connectedDevices.remove(address);
        this.connectedDeviceNames.remove(name);
    }

    // Devices who haven't ack'ed our START packet yet

    /**
     * Devices that have not acked our start yet.
     */
    private Set<String> unackedDevices;

    public void setupUnackedDevices() {
        unackedDevices.addAll(getConnectedDevices());
    }

    public synchronized void removeUnackedDevice(String deviceAddress) {
        unackedDevices.remove(deviceAddress);
    }

    public boolean isDoneAcking() {
        return this.unackedDevices.isEmpty();
    }

    // Turn-based bookkeeping

    /**
     * Stores whether or not this device has completed the current turn
     */
    private boolean isDone;

    public void setTurnDone(boolean done) {
        isDone = done;
    }

    // Devices who are not finished with their turns

    /**
     * A list of the devices that have not finished the current turn.
     */
    private Set<String> unfinishedDeviceList;

    public synchronized void deviceTurnFinished(String address) {
        unfinishedDeviceList.remove(address);
    }

    public synchronized void setupUnfinishedDevices(List<String> devices) {
        unfinishedDeviceList.addAll(devices);
    }

    public synchronized boolean isRoundOver() {
        return isDone && unfinishedDeviceList.isEmpty();
    }

    private GameData() {
        this.startDevice = Constants.NO_START;
        this.connectedDevices = new ArrayList<>();
        this.connectedDeviceNames = new ArrayList<>();
        this.unackedDevices = new HashSet<>();
        this.isDone = false;
        this.unfinishedDeviceList = new HashSet<>();
    }

    public static GameData getInstance() {
        return ourInstance;
    }

    // SETUP

    /**
     * Index of this device's successor in the list of connectedDevices
     */
    public static String successor = null;

    /**
     * Keeps track of how many devices have been chosen so far in that start game process
     */
    public static int lastSuccessorNumber = 0;

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
    public static String localAddress = null;

    /**
     * Our name.
     */
    public static String localName = BluetoothAdapter.getDefaultAdapter().getName();

    // IN-GAME

    /**
     * The timer for each turn.
     */
    public static CountDownTimer turnTimer = null;

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
