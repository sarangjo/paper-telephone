package com.cse461.a16au.papertelephone.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cse461.a16au.papertelephone.Constants;
import com.cse461.a16au.papertelephone.R;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Prompts the user to draw something and records the drawing that a user inputs,
 * forwards that drawing to GameActivity when the turn is complete
 */

public class DrawingFragment extends GameFragment {
    private Paint mPaint;
    private PaintingView paintingView;
    private String mCreatorAddress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        Bundle args = getArguments();
        if (args.getBoolean("start")) {
            mCreatorAddress = GameData.localAddress;
        } else {
            mCreatorAddress = args.getString(Constants.CREATOR_ADDRESS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_drawing, container, false);
        LinearLayout ll = (LinearLayout) view.findViewById(R.id.game_drawing);

        // Retrieve and display prompt
        Bundle args = getArguments();
        String prompt = args.getString("prompt");
        ((TextView) view.findViewById(R.id.drawing_help)).setText(prompt);

        // PaintingView for current drawing turn
        paintingView = new DrawingFragment.PaintingView(getActivity());
        paintingView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        ll.addView(paintingView);

        Button sendDrawingButton = (Button) view.findViewById(R.id.button_send_drawing);
        sendDrawingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endTurn();
            }
        });
        return view;
    }

    /**
     * Process the end of a turn, packaging the image and passing it through mListener to GameActivity
     */
    @Override
    public void endTurn() {
        // Convert image to byte array
        Bitmap cache = paintingView.getDrawingCache();
        Bitmap b = cache.copy(cache.getConfig(), true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] image = stream.toByteArray();

        // Save this to our paper store
        GameData.saveData(mCreatorAddress, image);

        // Create data packet to send
        ByteBuffer buf = ByteBuffer.allocate(Constants.HEADER_LENGTH + Constants.ADDRESS_LENGTH + image.length + 4);
        buf.put(Constants.HEADER_IMAGE);
        buf.put(mCreatorAddress.getBytes());
        buf.putInt(image.length);
        buf.put(image);

        mListener.sendTurnData(buf.array());
    }

    public class PaintingView extends View {
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private Path mPath;
        private Paint mBitmapPaint;

        public PaintingView(Context c) {
            super(c);
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            setDrawingCacheEnabled(true);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xFFAAAAAA);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
        }

        /**
         * The position of the user's finger on the screen.
         */
        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 1;

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
            }
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
            // commit the path to our offscreen
            mCanvas.drawPath(mPath, mPaint);
            // kill this so we don't double draw
            mPath.reset();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }
    }
}
