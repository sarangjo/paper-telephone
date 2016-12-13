package com.cse461.a16au.papertelephone.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.R;

import java.util.List;

/**
 * TODO: class documentation
 */

public class SummaryAdapter extends ArrayAdapter<Object> {
    private final List<Object> mValues;

    public SummaryAdapter(Context context, List<Object> values) {
        super(context, -1, values);
        mValues = values;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view;
        Object obj = mValues.get(position);
        if (obj != null && obj instanceof String) {
            TextView text = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            text.setText((String) obj);
            view = text;
        } else {
            ImageView image = (ImageView) inflater.inflate(R.layout.simple_image, parent, false);
            image.setImageBitmap((Bitmap) obj);
            view = image;
        }
        return view;
    }
}
