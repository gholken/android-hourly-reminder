package com.github.axet.hourlyreminder.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.basics.Alarm;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class Sound {
    public static final String TAG = Sound.class.getSimpleName();

    public final static Uri DEFAULT_ALARM = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

    // beep ms
    public static final int BEEP = 100;

    Context context;
    TextToSpeech tts;
    ToneGenerator tone;
    MediaPlayer player;
    AudioTrack track;
    Runnable delayed;
    Handler handler;
    Set<Runnable> done = new HashSet<>();

    Float volume;
    Runnable increaseVolume;

    // AudioSystem.STREAM_ALARM AudioManager.STREAM_ALARM;
    final static int SOUND_CHANNEL = AudioAttributes.USAGE_ALARM;
    final static int SOUND_TYPE = AudioAttributes.CONTENT_TYPE_SONIFICATION;

    public Sound(Context context) {
        this.context = context;

        handler = new Handler();

        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);

                    if (Build.VERSION.SDK_INT >= 21) {
                        tts.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(SOUND_CHANNEL)
                                .setContentType(SOUND_TYPE)
                                .build());
                    }

                    if (delayed != null) {
                        delayed.run();
                    }
                }
            }
        });
    }

    public void close() {
        playerClose();

        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        if (tone != null) {
            tone.release();
            tone = null;
        }
        if (track != null) {
            track.release();
            track = null;
        }
        if (delayed != null) {
            handler.removeCallbacks(delayed);
            delayed = null;
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

    public boolean silenced() {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        if (shared.getBoolean(HourlyApplication.PREFERENCE_CALLSILENCE, false)) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK) {
                return true;
            }
        }

        if (shared.getBoolean(HourlyApplication.PREFERENCE_MUSICSILENCE, false)) {
            AudioManager tm = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (tm.isMusicActive()) {
                return true;
            }
        }

        return false;
    }

    public void soundReminder(final long time) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        if (shared.getBoolean(HourlyApplication.PREFERENCE_VIBRATE, false)) {
            vibrate();
        }

        // do we have slince alarm?
        if (silenced()) {
            String text = "";
            text += context.getString(R.string.SoundSilenced);
            text += "\n";
            text += context.getResources().getString(R.string.TimeIs, Alarm.format(context, time));
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            return;
        }

        final Runnable speech = new Runnable() {
            @Override
            public void run() {
                if (shared.getBoolean(HourlyApplication.PREFERENCE_SPEAK, false)) {
                    playSpeech(time, null);
                } else {
                    timeToast(time);
                }
            }
        };

        final Runnable custom = new Runnable() {
            @Override
            public void run() {
                if (!shared.getString(HourlyApplication.PREFERENCE_CUSTOM_SOUND, "").equals(HourlyApplication.PREFERENCE_CUSTOM_SOUND_OFF)) {
                    playCustom(speech);
                } else {
                    speech.run();
                }
            }
        };

        final Runnable beep = new Runnable() {
            @Override
            public void run() {
                if (shared.getBoolean(HourlyApplication.PREFERENCE_BEEP, false)) {
                    playBeep(custom);
                } else {
                    custom.run();
                }
            }
        };

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
        player.setVolume(getRingtoneVolume(), getRingtoneVolume());
        player.start();

        increasedVolume(player);
    }

    public void increasedVolume(final MediaPlayer player) {
        if (increaseVolume != null)
            handler.removeCallbacks(increaseVolume);

        increaseVolume = new Runnable() {
            int step = 0;
            int steps = 50;
            int delay = 100;

            {
                SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
                int inc = Integer.parseInt(shared.getString(HourlyApplication.PREFERENCE_INCREASE_VOLUME, "0")) * 1000;

                steps = (inc / delay);
            }

            @Override
            public void run() {
                if (player == null)
                    return;

                float log1 = (float) (Math.log(steps - step) / Math.log(steps));
                float vol = 1 - log1;

                try {
                    player.setVolume(vol, vol);
                } catch (IllegalStateException e) {
                    // ignore. player probably already closed
                    return;
                }

                step++;

                if (step >= steps)
                    return;

                handler.postDelayed(increaseVolume, delay);
            }
        };

        increaseVolume.run();
    }

    float getRingtoneVolume() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        if (Integer.parseInt(shared.getString(HourlyApplication.PREFERENCE_INCREASE_VOLUME, "0")) > 0) {
            return 0;
        }

        return getVolume();
    }

    float getVolume() {
        if (volume != null)
            return volume;

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        return (float) (Math.pow(shared.getFloat(HourlyApplication.PREFERENCE_VOLUME, 1f), 3));
    }

    public void setVolume(float f) {
        volume = f;
    }

    public void playSpeech(final long time, final Runnable done) {
        Sound.this.done.clear();
        Sound.this.done.add(done);

        final Runnable clear = new Runnable() {
            @Override
            public void run() {
                if (delayed != null) {
                    handler.removeCallbacks(delayed);
                    delayed = null;
                }
                if (done != null && Sound.this.done.contains(done))
                    done.run();
            }
        };
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                clear.run();
            }

            @Override
            public void onError(String utteranceId) {
                clear.run();
            }
        });

        // TTS may say failed, but play sounds successfuly. we need regardless or failed do not
        // play speech twice if clear.run() was called.
        if (!playSpeech(time)) {
            Toast.makeText(context, context.getString(R.string.WaitTTS), Toast.LENGTH_SHORT).show();
            if (delayed != null) {
                handler.removeCallbacks(delayed);
            }
            delayed = new Runnable() {
                @Override
                public void run() {
                    if (!playSpeech(time)) {
                        Toast.makeText(context, context.getString(R.string.FailedTTS), Toast.LENGTH_SHORT).show();
                        clear.run();
                    }
                }
            };
            handler.postDelayed(delayed, 5000);
        }
    }

    boolean playSpeech(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);

        String speak;

        {
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            boolean speakAMPM = !DateFormat.is24HourFormat(context) && shared.getBoolean(HourlyApplication.PREFERENCE_SPEAK_AMPM, false);

            String ampm = "";
            if (speakAMPM) {
                ampm = hour >= 12 ? "PM" : "AM";
            }

            if (min != 0) {
                if (min < 10) {
                    speak = String.format("Time is %d o %d %s.", hour, min, ampm);
                } else {
                    speak = String.format("Time is %d %02d %s.", hour, min, ampm);
                }
            } else {
                if (speakAMPM)
                    speak = String.format("Time is %d o'%s", hour, ampm);
                else
                    speak = String.format("%d o'clock", hour);
            }
        }

        timeToast(time);

        if (Build.VERSION.SDK_INT >= 21) {
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, getVolume());
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DONE");
            if (tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString()) != TextToSpeech.SUCCESS) {
                return false;
            }
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, Float.toString(getVolume()));
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DONE");
            if (tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params) != TextToSpeech.SUCCESS) {
                return false;
            }
        }
        return true;
    }

    public void timeToast(long time) {
        String text = context.getResources().getString(R.string.TimeIs, Alarm.format(context, time));
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public void silencedToast() {
        String text = "";
        text += context.getString(R.string.SoundSilenced);
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

        player.setVolume(getRingtoneVolume(), getRingtoneVolume());
        player.start();

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
        if (player != null) {
            player.release();
            player = null;
            return true;
        }
        if (increaseVolume != null) {
            handler.removeCallbacks(increaseVolume);
            increaseVolume = null;
        }
        return false;
    }

    public void playAlarm(final Alarm a) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

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
    }
}
