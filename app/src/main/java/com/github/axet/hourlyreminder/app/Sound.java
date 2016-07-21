package com.github.axet.hourlyreminder.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.basics.Alarm;

public class Sound extends TTS {
    public static final String TAG = Sound.class.getSimpleName();

    ToneGenerator tone;
    MediaPlayer player;
    AudioTrack track;
    Runnable increaseVolume;

    public Sound(Context context) {
        super(context);
    }

    public void close() {
        super.close();

        playerClose();

        if (tone != null) {
            tone.release();
            tone = null;
        }
        if (track != null) {
            track.release();
            track = null;
        }
    }

    // https://gist.github.com/slightfoot/6330866
    private AudioTrack generateTone(double freqHz, int durationMs) {
        int sampleRate = 44100;
        int count = sampleRate * durationMs / 1000;
        int end = count;
        int stereo = count * 2;
        short[] samples = new short[stereo];
        for (int i = 0; i < stereo; i += 2) {
            short sample = (short) (Math.sin(2 * Math.PI * i / (sampleRate / freqHz)) * 0x7FFF);
            samples[i + 0] = sample;
            samples[i + 1] = sample;
        }
        // old phones bug.
        //
        // http://stackoverflow.com/questions/27602492
        //
        // with MODE_STATIC setNotificationMarkerPosition not called
        AudioTrack track = new AudioTrack(SOUND_CHANNEL, sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                stereo * (Short.SIZE / 8), AudioTrack.MODE_STREAM);
        track.write(samples, 0, stereo);
        if (track.setNotificationMarkerPosition(end) != AudioTrack.SUCCESS)
            throw new RuntimeException("unable to set marker");
        return track;
    }

    public Silenced silencedReminder() {
        Silenced ss = silenced();

        if (ss != Silenced.NONE)
            return ss;

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        boolean v = shared.getBoolean(HourlyApplication.PREFERENCE_VIBRATE, false);
        boolean c = !shared.getString(HourlyApplication.PREFERENCE_CUSTOM_SOUND, "").equals(HourlyApplication.PREFERENCE_CUSTOM_SOUND_OFF);
        boolean s = shared.getBoolean(HourlyApplication.PREFERENCE_SPEAK, false);
        boolean b = shared.getBoolean(HourlyApplication.PREFERENCE_BEEP, false);

        if (!v && !c && !s && !b)
            return Silenced.SETTINGS;

        if (v && !c && !s && !b)
            return Silenced.VIBRATE;

        return Silenced.NONE;
    }

    public Silenced silencedAlarm(Alarm a) {
        Silenced ss = silenced();

        if (ss != Silenced.NONE)
            return ss;

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        boolean v = shared.getBoolean(HourlyApplication.PREFERENCE_VIBRATE, false);
        boolean c = a.ringtone;
        boolean s = a.speech;
        boolean b = a.beep;

        if (!v && !c && !s && !b)
            return Silenced.SETTINGS;

        if (v && !c && !s && !b)
            return Silenced.VIBRATE;

        return Silenced.NONE;
    }

    public Silenced silenced() {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        if (shared.getBoolean(HourlyApplication.PREFERENCE_CALLSILENCE, false)) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
                return Silenced.CALL;
            }
        }

        if (shared.getBoolean(HourlyApplication.PREFERENCE_MUSICSILENCE, false)) {
            AudioManager tm = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (tm.isMusicActive()) {
                return Silenced.MUSIC;
            }
        }

        return Silenced.NONE;
    }

    public void soundReminder(final long time) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        Silenced s = silencedReminder();

        // do we have slince alarm?
        if (s != Silenced.NONE) {
            if (s == Silenced.VIBRATE)
                vibrate();

            String text = "";
            switch (s) {
                case VIBRATE:
                    text += context.getString(R.string.SoundSilencedVibrate);
                    break;
                case CALL:
                    text += context.getString(R.string.SoundSilencedCall);
                    break;
                case MUSIC:
                    text += context.getString(R.string.SoundSilencedMusic);
                    break;
                case SETTINGS:
                    text += context.getString(R.string.SoundSilencedSettings);
                    break;
            }
            text += "\n";
            text += context.getResources().getString(R.string.ToastTime, Alarm.format(context, time));

            Toast t = Toast.makeText(context, text, Toast.LENGTH_SHORT);
            TextView v = (TextView) t.getView().findViewById(android.R.id.message);
            if (v != null)
                v.setGravity(Gravity.CENTER);
            t.show();
            return;
        }

        if (shared.getBoolean(HourlyApplication.PREFERENCE_VIBRATE, false)) {
            vibrate();
        }

        final Runnable custom = new Runnable() {
            @Override
            public void run() {
                if (!shared.getString(HourlyApplication.PREFERENCE_CUSTOM_SOUND, "").equals(HourlyApplication.PREFERENCE_CUSTOM_SOUND_OFF)) {
                    playCustom(null);
                }
            }
        };

        final Runnable speech = new Runnable() {
            @Override
            public void run() {
                if (shared.getBoolean(HourlyApplication.PREFERENCE_SPEAK, false)) {
                    playSpeech(time, custom);
                } else {
                    custom.run();
                }
            }
        };

        final Runnable beep = new Runnable() {
            @Override
            public void run() {
                if (shared.getBoolean(HourlyApplication.PREFERENCE_BEEP, false)) {
                    playBeep(speech);
                } else {
                    speech.run();
                }
            }
        };

        timeToast(time);

        beep.run();
    }

    public void playCustom(final Runnable done) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        String custom = shared.getString(HourlyApplication.PREFERENCE_CUSTOM_SOUND, "");

        if (custom.equals("ringtone")) {
            String uri = shared.getString(HourlyApplication.PREFERENCE_RINGTONE, "");
            playerClose();

            Sound.this.done.clear();
            Sound.this.done.add(done);

            if (uri.isEmpty()) {
                if (done != null)
                    done.run();
            } else {
                player = playOnce(Uri.parse(uri), new Runnable() {
                    @Override
                    public void run() {
                        if (done != null && Sound.this.done.contains(done))
                            done.run();
                        playerClose();
                    }
                });
            }
        } else if (custom.equals("sound")) {
            String uri = shared.getString(HourlyApplication.PREFERENCE_SOUND, "");
            playerClose();

            Sound.this.done.clear();
            Sound.this.done.add(done);

            if (uri.isEmpty()) {
                if (done != null)
                    done.run();
            } else {
                player = playOnce(Uri.parse(uri), new Runnable() {
                    @Override
                    public void run() {
                        if (done != null && Sound.this.done.contains(done))
                            done.run();
                        playerClose();
                    }
                });
            }
        } else {
            if (done != null)
                done.run();
        }
    }

    public void playBeep(final Runnable done) {
        if (track != null)
            track.release();

        track = generateTone(900, BEEP);

        if (Build.VERSION.SDK_INT < 21) {
            track.setStereoVolume(getVolume(), getVolume());
        } else {
            track.setVolume(getVolume());
        }

        Sound.this.done.clear();
        Sound.this.done.add(done);

        track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack t) {
                // prevent strange android bug, with second beep when connecting android to external usb audio source.
                // seems like this beep pushed to external audio source from sound cache.
                if (track != null) {
                    track.release();
                    track = null;
                }
                if (done != null && Sound.this.done.contains(done))
                    done.run();
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
            }
        });

        track.play();
    }

    public void playRingtone(Uri uri) {
        playerClose();
        player = MediaPlayer.create(context, uri);
        if (player == null) {
            player = MediaPlayer.create(context, Alarm.DEFAULT_RING);
        }
        if (player == null) {
            if (tone != null) {
                tone.release();
            }
            tone = new ToneGenerator(SOUND_CHANNEL, 100);
            tone.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL);
            return;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(SOUND_CHANNEL)
                    .setContentType(SOUND_TYPE)
                    .build());
        }
        player.setLooping(true);

        startPlayer(player);
    }

    public void startPlayer(final MediaPlayer player) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        final int inc = Integer.parseInt(shared.getString(HourlyApplication.PREFERENCE_INCREASE_VOLUME, "0")) * 1000;

        if (inc == 0) {
            player.setVolume(getVolume(), getVolume());
            player.start();

            return;
        }

        final float startVolume;

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        float systemVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM) / (float) am.getStreamVolume(AudioManager.STREAM_ALARM);
        float alarmVolume = getVolume();

        // if user trying to reduce alarms volume, then use it as start volume. else start from silence
        if (systemVolume > alarmVolume)
            startVolume = alarmVolume;
        else
            startVolume = 0;

        if (increaseVolume != null)
            handler.removeCallbacks(increaseVolume);

        increaseVolume = new Runnable() {
            int step = 0;
            int steps = 50;
            int delay = 100;
            // we start from startVolume, rest - how much we should increase
            float rest = 0;

            {
                steps = (inc / delay);
                rest = 1f - startVolume;
            }

            @Override
            public void run() {
                if (player == null)
                    return;

                float log1 = (float) (Math.log(steps - step) / Math.log(steps));
                // volume 0..1
                float vol = 1 - log1;

                // actual volume
                float restvol = startVolume + rest * vol;

                try {
                    player.setVolume(restvol, restvol);
                } catch (IllegalStateException e) {
                    // ignore. player probably already closed
                    return;
                }

                step++;

                if (step >= steps) {
                    // should be clear anyway
                    handler.removeCallbacks(increaseVolume);
                    increaseVolume = null;
                    Log.d(TAG, "increaseVolume done");
                    return;
                }

                handler.postDelayed(increaseVolume, delay);
            }
        };

        increaseVolume.run();
        player.start();
    }

    public void timeToast(long time) {
        String text = context.getResources().getString(R.string.ToastTime, Alarm.format(context, time));
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public void silencedToast(Silenced s) {
        String text = "";
        switch (s) {
            case CALL:
                text += context.getString(R.string.SoundSilencedCall);
                break;
            case MUSIC:
                text += context.getString(R.string.SoundSilencedMusic);
                break;
            case SETTINGS:
                text += context.getString(R.string.SoundSilencedSettings);
                break;
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public MediaPlayer playOnce(Uri uri, final Runnable done) {
        MediaPlayer player = MediaPlayer.create(context, uri);
        if (player == null) {
            player = MediaPlayer.create(context, DEFAULT_ALARM);
        }
        if (player == null) {
            Toast.makeText(context, context.getString(R.string.NoDefaultRingtone), Toast.LENGTH_SHORT).show();
            if (done != null)
                done.run();
            return null;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(SOUND_CHANNEL)
                    .setContentType(SOUND_TYPE)
                    .build());
        }

        // https://code.google.com/p/android/issues/detail?id=1314
        player.setLooping(false);

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                           @Override
                                           public void onCompletion(MediaPlayer mp) {
                                               if (done != null)
                                                   done.run();
                                           }
                                       }
        );

        startPlayer(player);

        return player;
    }

    public void vibrate() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(400);
    }

    public void vibrateStart() {
        long[] pattern = {0, 1000, 300};
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(pattern, 0);
    }

    public void vibrateStop() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.cancel();
    }

    public boolean playerClose() {
        done.clear();

        if (increaseVolume != null) {
            handler.removeCallbacks(increaseVolume);
            increaseVolume = null;
        }

        if (player != null) {
            player.release();
            player = null;
            return true;
        }

        return false;
    }

    public Silenced playAlarm(final Alarm a) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        Silenced s = silencedAlarm(a);

        if (s == Silenced.VIBRATE) {
            vibrateStart();
            return s;
        }

        if (s != Silenced.NONE)
            return s;

        if (shared.getBoolean(HourlyApplication.PREFERENCE_VIBRATE, false)) {
            vibrateStart();
        }

        final long time = System.currentTimeMillis();

        if (a.beep) {
            playBeep(new Runnable() {
                         @Override
                         public void run() {
                             if (a.speech) {
                                 playSpeech(time, new Runnable() {
                                     @Override
                                     public void run() {
                                         if (a.ringtone) {
                                             playRingtone(Uri.parse(a.ringtoneValue));
                                         }
                                     }
                                 });
                             } else if (a.ringtone) {
                                 playRingtone(Uri.parse(a.ringtoneValue));
                             }
                         }
                     }
            );
        } else if (a.speech) {
            playSpeech(time, new Runnable() {
                @Override
                public void run() {
                    playRingtone(Uri.parse(a.ringtoneValue));
                }
            });
        } else if (a.ringtone) {
            playRingtone(Uri.parse(a.ringtoneValue));
        }

        return s;
    }
}
