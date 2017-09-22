package com.cse461.a16au.papertelephone.game;

import android.bluetooth.BluetoothAdapter;
import android.os.CountDownTimer;

import com.cse461.a16au.papertelephone.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/** TODO: class documentation */
public class GameData {

  private static final GameData ourInstance = new GameData();

  // The start device
  public static boolean doesEndOnPrompt;
  /** Index of this device's successor in the list of connectedDevices */
  public static String successor = null;
  /** Keeps track of how many devices have been chosen so far in that start game process */
  public static int lastSuccessorNumber = 0;

  // Devices we are currently connected to
  /** Set of devices that have not been chosen as a successor in the start game process */
  public static Set<String> unplacedDevices = new HashSet<>();
  /** Keeps track of how many turns remain until the end of the game */
  public static int turnsLeft = 0;
  /** This device's mac address */
  public static String localAddress = null;
  /** Our name. */
  public static String localName = BluetoothAdapter.getDefaultAdapter().getName();
  /** The timer for each turn. */
  public static CountDownTimer turnTimer = null;
  public static ConnectionChangeListener connectionChangeListener;
  public static List<String> devicesAtStartGame;
  public static List<String> namesAtStartGame;
  /**
   * Map from device addresses to their "Summaries" which store the original prompt, along with the
   * drawings and prompts that followed it
   */
  public static ConcurrentMap<String, List<byte[]>> addressToSummaries;
  /** Index of the device that started the game, -1 if it is this device */
  private String startDevice;
  /** List of all connected device address */
  private List<String> connectedDevices;

  // Devices we need to connect to in order to be part of the group
  /** List of all lobbied devices */
  private Set<String> lobbiedDevices;
  /** List of all connected device names */
  private List<String> connectedDeviceNames;
  private Set<String> devicesToConnectTo;
  /** Devices that have not acked our start yet. */
  private Set<String> unackedDevices;

  // Devices who haven't ack'ed our START packet yet
  /** Stores whether or not this device has completed the current turn */
  private boolean isDone;
  /** A list of the devices that have not finished the current turn. */
  private Set<String> unfinishedDeviceList;

  private GameData() {
    this.startDevice = Constants.NO_START;
    this.connectedDevices = new ArrayList<>();
    this.connectedDeviceNames = new ArrayList<>();
    this.unackedDevices = new HashSet<>();
    this.isDone = false;
    this.unfinishedDeviceList = new HashSet<>();
    this.devicesToConnectTo = new HashSet<>();
    this.lobbiedDevices = new HashSet<>();
  }

  public static GameData getInstance() {
    return ourInstance;
  }

  // Turn-based bookkeeping

  public static void saveData(String creatorAddress, byte[] data) {
    List<byte[]> summary;
    if (addressToSummaries.containsKey(creatorAddress)) {
      summary = addressToSummaries.get(creatorAddress);
    } else {
      summary = new ArrayList<>();
    }
    // Checking if we are double-receiving the same data
    if (summary.size() == 0 || !Arrays.equals(summary.get(summary.size() - 1), data)) {
      summary.add(data);
    }
    addressToSummaries.put(creatorAddress, summary);
  }

  public synchronized String getStartDevice() {
    return startDevice;
  }

  public synchronized void setStartDevice(String startDevice) {
    this.startDevice = startDevice;
  }

  public List<String> getConnectedDevices() {
    return Collections.unmodifiableList(connectedDevices);
  }

  // Devices who are not finished with their turns

  public List<String> getConnectedDeviceNames() {
    return Collections.unmodifiableList(connectedDeviceNames);
  }

  public synchronized void addConnectedDevice(String address, String name) {
    this.connectedDevices.add(address);
    this.connectedDeviceNames.add(name);
  }

  public synchronized void addLobbiedDevice(String address) {
    this.lobbiedDevices.add(address);
  }

  public Set<String> getLobbiedDevices() {
    return Collections.unmodifiableSet(lobbiedDevices);
  }

  public void clearLobbiedDevices() {
    this.lobbiedDevices.clear();
  }

  public synchronized void removeLobbiedDevice(String address) {
    this.lobbiedDevices.remove(address);
  }

  public synchronized void removeConnectedDevice(String address, String name) {
    this.connectedDevices.remove(address);
    this.connectedDeviceNames.remove(name);
  }

  // SETUP

  public synchronized void addDeviceToConnectTo(String address) {
    devicesToConnectTo.add(address);
  }

  public synchronized void removeDeviceToConnectTo(String address) {
    devicesToConnectTo.remove(address);
  }

  public synchronized boolean isDoneConnectingToGameDevices() {
    return devicesToConnectTo.isEmpty();
  }

  public void setupUnackedDevices() {
    unackedDevices.addAll(getConnectedDevices());
  }

  public synchronized void removeUnackedDevice(String deviceAddress) {
    unackedDevices.remove(deviceAddress);
  }

  public boolean isDoneAcking() {
    return this.unackedDevices.isEmpty();
  }

  // IN-GAME

  public boolean getTurnDone() {
    return isDone;
  }

  public void setTurnDone(boolean done) {
    isDone = done;
  }

  // END-GAME

  public synchronized void deviceTurnFinished(String address) {
    unfinishedDeviceList.remove(address);
  }

  public synchronized void setupUnfinishedDevices(List<String> devices) {
    unfinishedDeviceList.addAll(devices);
  }

  public synchronized void addUnfinishedDevice(String address) {
    unfinishedDeviceList.add(address);
  }

  public synchronized boolean isRoundOver() {
    return isDone && unfinishedDeviceList.isEmpty();
  }
}
