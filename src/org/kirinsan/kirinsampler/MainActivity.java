package org.kirinsan.kirinsampler;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SeekBarProgressChange;
import org.androidannotations.annotations.ViewById;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;

@SuppressLint("SetJavaScriptEnabled")
@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.activity_main)
public class MainActivity extends Activity {
	private SoundPool sp;
	private static final Map<String, Integer> map = new HashMap<String, Integer>();
	private SocketIO socket;
	private float playRate = 1.0f;
	private boolean socketEnabled = true;

	@ViewById
	WebView webview;

	@AfterViews
	void init() {
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		webview.getSettings().setJavaScriptEnabled(true);
		webview.addJavascriptInterface(this, "android");
		webview.setWebViewClient(new WebViewClient());
		webview.setWebChromeClient(new WebChromeClient());

		webview.loadUrl("file:///android_asset/index.html");
	}

	@Override
	protected void onResume() {
		super.onResume();

		loadSounds();
		connectSocketIO();
	}

	@Background
	void loadSounds() {
		sp = new SoundPool(16, AudioManager.STREAM_MUSIC, 1);

		for (Field field : R.raw.class.getFields()) {
			try {
				int id = sp.load(this, field.getInt(null), 0);
				String name = field.getName();
				map.put(name, id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Background
	void connectSocketIO() {
		try {
			socket = new SocketIO();
			socket.connect("http://world.kirinsan.org:4040", new IOCallback() {
				@Override
				public void onMessage(JSONObject arg0, IOAcknowledge arg1) {
				}

				@Override
				public void onMessage(String arg0, IOAcknowledge arg1) {
				}

				@Override
				public void onError(SocketIOException arg0) {
				}

				@Override
				public void onDisconnect() {
				}

				@Override
				public void onConnect() {
				}

				@Override
				public void on(String message, IOAcknowledge arg1, Object... id) {
					if (socketEnabled) {
						if ("playById".equals(message)) {
							play(id[0].toString());
						}
					}
				}
			});
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (sp != null) {
			sp.release();
			sp = null;
		}

		if (socket != null) {
			socket.disconnect();
			socket = null;
		}
	}

	@JavascriptInterface
	public void play(String id) {
		sp.play(map.get(id), 1.0f, 1.0f, 1, 0, playRate);
	}

	@JavascriptInterface
	public void emit(String id) {
		socket.emit("playById", id);
	}

	@JavascriptInterface
	public boolean isSocketEnabled() {
		return socketEnabled;
	}

	@SeekBarProgressChange(R.id.bar)
	void barChanged(SeekBar bar, int progress) {
		playRate = 0.5f + (float) progress / 100.0f;
	}

	@OptionsItem
	void kirinsanOrg() {
		startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse("http://kirinsan.org")));
	}

	@OptionsItem
	void share() {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name);
		i.putExtra(Intent.EXTRA_TEXT, getText(R.string.share_text) + " https://play.google.com/store/apps/details?id=org.kirinsan.kirinsampler #kirinsan.org");
		startActivity(Intent.createChooser(i, getText(R.string.share)));
	}

	@OptionsItem
	void socketSwitch(MenuItem item) {
		socketEnabled = !socketEnabled;

		if (socketEnabled) {
			item.setTitle(R.string.socket_enabled);
		} else {
			item.setTitle(R.string.socket_disabled);
		}
	}
}
