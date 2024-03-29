/*
 * Copyright 2007 ZXing authors
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

package at.ftw.mabs.scanner;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.ui.AugmentedRealityActivity;

import com.google.zxing.result.Result;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ActivityHandler extends Handler {
	private static final String				TAG	= "MABS/ActivityHandler";

	private final AugmentedRealityActivity	activity;
	private final DecodeThread				decodeThread;
	private State							state;

	private enum State {
		PREVIEW,
		SUCCESS,
		DONE
	}

	public ActivityHandler(AugmentedRealityActivity augmentedViewActivity, boolean beginScanning) {
		this.activity = augmentedViewActivity;

		decodeThread = new DecodeThread(augmentedViewActivity);
		decodeThread.start();
		state = State.SUCCESS;

		// Start ourselves capturing previews and decoding.
		CameraManager.get().startPreview();
		if (beginScanning) {
			restartPreviewAndDecode();
		}
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
			case R.id.auto_focus:
				// When one auto focus pass finishes, start another. This is the
				// closest thing to
				// continuous AF. It does seem to hunt a bit, but I'm not sure
				// what else to do.
				if (state == State.PREVIEW) {
					CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
				}
				break;
			case R.id.restart_preview:
				restartPreviewAndDecode();
				break;
			case R.id.decode_succeeded:
				state = State.SUCCESS;
				Bundle bundle = message.getData();
				Bitmap barcode = bundle == null ? null : (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
				activity.handleDecode((Result) message.obj, barcode);
				break;
			case R.id.decode_failed:
				// We're decoding as fast as possible, so when one decode fails,
				// start another.
				state = State.PREVIEW;
				CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
				break;
			case R.id.return_scan_result:
				activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
				activity.finish();
				break;
		}
	}

	public void quitSynchronously() {
		state = State.DONE;

		CameraManager.get().stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
		quit.sendToTarget();

		try {
			decodeThread.join();
		} catch (InterruptedException e) {
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(R.id.decode_succeeded);
		removeMessages(R.id.decode_failed);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;

			CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.decode);
			CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
		}
	}
}
