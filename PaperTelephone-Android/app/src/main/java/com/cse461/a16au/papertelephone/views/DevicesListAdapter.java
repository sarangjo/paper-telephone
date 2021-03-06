package com.cse461.a16au.papertelephone.views;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Displays list of paired devices and newly discovered devices. */
class DevicesListAdapter extends BaseExpandableListAdapter {
  private List<String> pairedDevices;
  private List<String> newDevices;
  private LayoutInflater inflater;

  DevicesListAdapter(Activity act) {
    pairedDevices = new ArrayList<>();
    newDevices = new ArrayList<>();
    inflater = act.getLayoutInflater();
  }

  @Override
  public int getGroupCount() {
    return 2;
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    return (getGroup(groupPosition)).size();
  }

  @Override
  public List<String> getGroup(int groupPosition) {
    switch (groupPosition) {
      case 0:
        return pairedDevices;
      case 1:
        return newDevices;
      default:
        return null;
    }
  }

  @Override
  public Object getChild(int groupPosition, int childPosition) {
    return (getGroup(groupPosition)).get(childPosition);
  }

  @Override
  public long getGroupId(int groupPosition) {
    return groupPosition;
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return childPosition;
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public View getGroupView(
      int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
    View v = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
    ((TextView) v.findViewById(android.R.id.text1))
        .setText(String.format("%s", (groupPosition == 0 ? "Paired" : "New") + " Devices"));
    return v;
  }

  @Override
  public View getChildView(
      int groupPosition,
      int childPosition,
      boolean isLastChild,
      View convertView,
      ViewGroup parent) {
    View v = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null);
    ((TextView) v.findViewById(android.R.id.text1))
        .setText((getGroup(groupPosition)).get(childPosition));
    return v;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  void addPairedDevice(String s) {
    this.pairedDevices.add(s);
    this.notifyDataSetChanged();
  }

  void addNewDevice(String s) {
    this.newDevices.add(s);
    this.notifyDataSetChanged();
  }

  void clearNew() {
    this.newDevices.clear();
    this.notifyDataSetChanged();
  }
}
