package at.ftw.mabs;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import at.ftw.mabs.camera.AugmentedView;

public class ShowAugmentedView extends Activity {
	private AugmentedView	previewView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Hide the window title.
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Create our Preview view and set it as the content of our activity.
		previewView = new AugmentedView(this);
		setContentView(previewView);
	}
}