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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private long offset; // サーバーとの時差

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

        final FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference(".info/serverTimeOffset").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                offset = dataSnapshot.getValue(Long.TYPE);

                db.getReference("sound").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        SoundInfo sound = snapshot.getValue(SoundInfo.class);
                        if (sound != null) {
                            long timeFromLastSound = Math.abs(sound.time - estimatedServerTime());

                            // Ignore far past events
                            if (timeFromLastSound < 1000) {
                                play(sound.id);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    /**
     * サーバーの時刻を返す。
     *
     * @return
     */
    private long estimatedServerTime() {
        return System.currentTimeMillis() + offset;
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
    public void setSoundId(String id) {
        FirebaseDatabase.getInstance().getReference("sound").setValue(new SoundInfo(id, estimatedServerTime()));
    }

    private void play(String id) {
        Integer soundID = map.get(id);
        if (soundID != null) {
            sp.play(soundID, 1.0f, 1.0f, 1, 0, playRate);
        }
    }

    @SeekBarProgressChange(R.id.bar)
    void barChanged(SeekBar bar, int progress) {
        playRate = 0.5f + (float) progress / 100.0f;
    }
}
