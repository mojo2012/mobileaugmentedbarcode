/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.ftw.mabs.ui.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {
	private static final String	TAG				= "MABS/ViewfinderView";
	private static final int[]	SCANNER_ALPHA	= { 0, 64, 128, 192, 255, 192, 128, 64 };
	private static final long	ANIMATION_DELAY	= 100L;

	private final Paint			paint;
	private final Rect			outerBox;
	private final Rect			innerBox;
	private final int			maskColor;
	private final int			resultColor;
	private final int			frameColor;
	private final int			laserColor;
	private final int			scannerAlpha;

	// This constructor is used when the class is built from an XML resource.
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint();
		outerBox = new Rect();
		innerBox = new Rect();
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);
		frameColor = Color.WHITE;
		laserColor = resources.getColor(R.color.viewfinder_laser);
		scannerAlpha = 0;
		paint.setStrokeWidth(2.0f);
		paint.setStyle(Style.STROKE);
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = CameraManager.get().getFramingRect();

		if (frame == null) {
			return;
		}

		// Draw a two pixel solid black border inside the framing rect
		paint.setColor(Color.BLACK);
		outerBox.set(frame.left + 19, frame.top + 19, frame.right - 19, frame.bottom - 19);
		canvas.drawRect(outerBox, paint);

		paint.setColor(Color.WHITE);
		innerBox.set(frame.left + 21, frame.top + 21, frame.right - 21, frame.bottom - 21);
		canvas.drawRect(innerBox, paint);

		// Draw a red "laser scanner" line through the middle to show decoding
		// is active
		// paint.setStyle(Style.FILL);
		// paint.setColor(laserColor);
		// paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
		// scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
		// int middle = frame.height() / 2 + frame.top;
		// outerBox.set(frame.left + 2, middle - 1, frame.right - 1, middle +
		// 2);
		// canvas.drawRect(outerBox, paint);

		// Request another update at the animation interval, but only repaint
		// the laser line, not the entire viewfinder mask.
		postInvalidateDelayed(ANIMATION_DELAY, outerBox.left, outerBox.top, outerBox.right, outerBox.bottom);
	}

	public void drawViewfinder() {
		invalidate();
	}
}
