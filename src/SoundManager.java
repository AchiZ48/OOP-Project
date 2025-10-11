import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class SoundManager {
    enum Channel {
        AMBIENT,
        BATTLE,
        UI
    }

    private final Map<String, Clip> clipCache = new HashMap<>();
    private Clip ambientClip;
    private Clip battleClip;
    private boolean muted = false;
    private float masterGain = 0.8f;

    void setMuted(boolean muted) {
        this.muted = muted;
        refreshVolumes();
    }

    void setMasterGain(float gain) {
        masterGain = Math.max(0f, Math.min(1f, gain));
        refreshVolumes();
    }

    void playAmbient(String trackId) {
        ambientClip = playLoop(trackId, ambientClip);
    }

    void playBattle(String trackId) {
        battleClip = playLoop(trackId, battleClip);
    }

    boolean isChannelPlaying(Channel channel) {
        return switch (channel) {
            case AMBIENT -> ambientClip != null && ambientClip.isActive();
            case BATTLE -> battleClip != null && battleClip.isActive();
            case UI -> false;
        };
    }

    void stopChannel(Channel channel) {
        switch (channel) {
            case AMBIENT -> stopClip(ambientClip);
            case BATTLE -> stopClip(battleClip);
            case UI -> { /* reserved for later */ }
        }
    }

    void stopAll() {
        stopClip(ambientClip);
        stopClip(battleClip);
    }

    void playSfx(String sfxId) {
        Clip clip = loadClip(sfxId);
        if (clip == null) {
            return;
        }
        clip.stop();
        clip.setFramePosition(0);
        applyVolume(clip);
        clip.start();
    }

    private Clip playLoop(String trackId, Clip existingClip) {
        Clip clip = loadClip(trackId);
        if (clip == null) {
            return existingClip;
        }
        if (clip == existingClip && clip.isActive()) {
            return clip;
        }
        stopClip(existingClip);
        clip.stop();
        clip.setFramePosition(0);
        applyVolume(clip);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        return clip;
    }

    private Clip loadClip(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        Clip cached = clipCache.get(id);
        if (cached != null) {
            return cached;
        }
        String basePath = "resources/audio/" + id + ".wav";
        try (AudioInputStream stream = openAudioStream(basePath)) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            clipCache.put(id, clip);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Audio load failed for " + id + ": " + e.getMessage());
            return null;
        }
    }

    private void stopClip(Clip clip) {
        if (clip != null) {
            clip.stop();
            clip.flush();
        }
    }

    private void refreshVolumes() {
        applyVolume(ambientClip);
        applyVolume(battleClip);
        for (Clip clip : clipCache.values()) {
            applyVolume(clip);
        }
    }

    private void applyVolume(Clip clip) {
        if (clip == null) {
            return;
        }
        FloatControl control;
        try {
            control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        } catch (IllegalArgumentException ex) {
            return;
        }
        float gain = muted ? control.getMinimum() : linearToDecibel(masterGain);
        control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), gain)));
    }

    private float linearToDecibel(float value) {
        if (value <= 0f) {
            return -80f;
        }
        return (float) (20.0 * Math.log10(value));
    }

    private AudioInputStream openAudioStream(String resourcePath) throws UnsupportedAudioFileException, IOException {
        URL url = ResourceLoader.getResourceUrl(resourcePath);
        if (url != null) {
            return AudioSystem.getAudioInputStream(url);
        }
        return AudioSystem.getAudioInputStream(new java.io.File(resourcePath));
    }
}
