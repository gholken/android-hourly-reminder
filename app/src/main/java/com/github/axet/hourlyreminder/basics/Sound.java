package com.github.axet.hourlyreminder.basics;

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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import com.github.axet.hourlyreminder.HourlyApplication;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class Sound {
    // beep ms
    public static final int BEEP = 100;

    Context context;
    TextToSpeech tts;
    ToneGenerator tone;
    MediaPlayer player;

    public Sound(Context context) {
        this.context = context;

        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);

                    if (Build.VERSION.SDK_INT >= 21) {
                        tts.setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build());
                    }
                }
            }
        });
    }

    public void close() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    // https://gist.github.com/slightfoot/6330866
    private AudioTrack generateTone(double freqHz, int durationMs) {
        int count = (int) (44100.0 * (durationMs / 1000.0)) & ~1;
        int end = count;

        count = count * 2;
        short[] samples = new short[count];
        for (int i = 0; i < count; i += 2) {
            short sample = (short) (Math.sin(2 * Math.PI * i / (44100.0 / freqHz)) * 0x7FFF);
            samples[i + 0] = sample;
            samples[i + 1] = sample;
        }
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                count * (Short.SIZE / 8), AudioTrack.MODE_STATIC);
        track.write(samples, 0, count);
        track.setNotificationMarkerPosition(end);
        return track;
    }

    public void soundAlarm() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        if (shared.getBoolean("beep", false)) {
            playBeep(new Runnable() {
                @Override
                public void run() {
                    playSpeech(null);
                }
            });
        } else {
            playSpeech(null);
        }
    }

    public void playBeep(final Runnable done) {
        AudioTrack track = generateTone(900, BEEP);

        if (Build.VERSION.SDK_INT < 21) {
            track.setStereoVolume(getVolume(), getVolume());
        } else {
            track.setVolume(getVolume());
        }

        track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onMarkerReached(AudioTrack track) {
                if (done != null)
                    done.run();
            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
            }
        });

        track.play();
    }

    public void playRingtone(Uri uri) {
        if (player != null) {
            player.release();
        }
        player = MediaPlayer.create(context, uri);
        if (player == null) {
            player = MediaPlayer.create(context, Uri.parse(Alarm.DEFAULT_RING));
        }
        if (player == null) {
            tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            tone.startTone(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL);
            return;
        }
        player.setLooping(true);
        player.setVolume(getVolume(), getVolume());
        player.start();
    }

    float getVolume() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        return (float) (Math.pow(shared.getFloat("volume", 1f), 3));
    }

    public void playSpeech(final Runnable run) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int min = Calendar.getInstance().get(Calendar.MINUTE);

        String text = String.format("Time is %d:%d.", hour, min);

        String speak;
        if (min != 0) {
            if (min < 10) {
                speak = String.format("Time is %d o %d.", hour, min);
            } else {
                speak = String.format("Time is %d %02d.", hour, min);
            }
        } else
            speak = String.format("%d o'clock", hour);

        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                if (run != null)
                    run.run();
            }

            @Override
            public void onError(String utteranceId) {
                if (run != null)
                    run.run();
            }
        });

        if (Build.VERSION.SDK_INT >= 21) {
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, getVolume());
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DONE");
            tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString());
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, Float.toString(getVolume()));
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "DONE");
            tts.speak(speak, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    public MediaPlayer playOnce(Uri uri) {
        // https://code.google.com/p/android/issues/detail?id=1314
        MediaPlayer player = MediaPlayer.create(context, uri);
        if (player == null) {
            player = MediaPlayer.create(context, Uri.parse(Alarm.DEFAULT_RING));
        }
        if (player == null) {
            Toast.makeText(context, "No default ringtone", Toast.LENGTH_SHORT).show();
            return null;
        }
        final MediaPlayer p = player;
        player.setLooping(false);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                           @Override
                                           public void onCompletion(MediaPlayer mp) {
                                               p.stop();
                                               p.release();
                                           }
                                       }
        );
        player.setVolume(getVolume(), getVolume());
        player.start();
        return player;
    }

}
