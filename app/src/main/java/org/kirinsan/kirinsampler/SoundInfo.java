package org.kirinsan.kirinsampler;

/**
 * /sound に書き込まれる内容
 */
public class SoundInfo {
    public String id;
    public long time;

    public SoundInfo() {
    }

    public SoundInfo(String id, long time) {
        this.id = id;
        this.time = time;
    }
}
