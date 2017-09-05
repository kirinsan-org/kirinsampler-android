package org.kirinsan.kirinsampler;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SeekBarProgressChange;
import org.androidannotations.annotations.ViewById;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
@EActivity(R.layout.activity_sampler)
public class MainActivity extends AppCompatActivity {

    private SoundPool sp;
    private static final Map<String, Integer> map = new HashMap<String, Integer>();
    private float playRate = 1.0f;

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

    @Override
    protected void onPause() {
        super.onPause();

        if (sp != null) {
            sp.release();
            sp = null;
        }

    }

    @JavascriptInterface
    public void play(String id) {
        sp.play(map.get(id), 1.0f, 1.0f, 1, 0, playRate);
    }

    @SeekBarProgressChange(R.id.bar)
    void barChanged(SeekBar bar, int progress) {
        playRate = 0.5f + (float) progress / 100.0f;
    }
}
