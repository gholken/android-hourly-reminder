package com.github.axet.hourlyreminder.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.github.axet.hourlyreminder.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class TTS extends SoundConfig {
    public static final String TAG = TTS.class.getSimpleName();

    public static final int DELAYED_DELAY = 5000;

    TextToSpeech tts;
    Runnable delayed; // tts may not be initalized, on init done, run delayed.run()
    boolean restart; // restart tts once if failed. on apk upgrade tts failed connection.
    Set<Runnable> done = new HashSet<>();

    public TTS(Context context) {
        super(context);

        ttsCreate();
    }

    void ttsCreate() {
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
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        if (delayed != null) {
            handler.removeCallbacks(delayed);
            delayed = null;
        }
    }

    public void playSpeech(final long time, final Runnable done) {
        TTS.this.done.clear();
        TTS.this.done.add(done);

        // clear delayed(), sound just played
        final Runnable clear = new Runnable() {
            @Override
            public void run() {
                if (delayed != null) {
                    handler.removeCallbacks(delayed);
                    delayed = null;
                }
                if (done != null && TTS.this.done.contains(done))
                    done.run();
            }
        };

        if (tts == null) {
            ttsCreate();
        }

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
                        tts.shutdown(); // on apk upgrade tts failed always. close it and restart.
                        tts = null;
                        if (restart) {
                            Toast.makeText(context, context.getString(R.string.FailedTTS), Toast.LENGTH_SHORT).show();
                            clear.run();
                        } else {
                            restart = true;
                            Toast.makeText(context, context.getString(R.string.FailedTTSRestar), Toast.LENGTH_SHORT).show();
                            if (delayed != null) {
                                handler.removeCallbacks(delayed);
                            }
                            delayed = new Runnable() {
                                @Override
                                public void run() {
                                    playSpeech(time, done);
                                }
                            };
                            handler.postDelayed(delayed, DELAYED_DELAY);
                        }
                    }
                }
            };
            handler.postDelayed(delayed, DELAYED_DELAY);
        }
    }

    boolean playSpeech(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int h = DateFormat.is24HourFormat(context) ? hour : c.get(Calendar.HOUR);
        int min = c.get(Calendar.MINUTE);

        String speak = "";

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        boolean is24 = DateFormat.is24HourFormat(context);
        boolean speakAMPMFlag = !is24 && shared.getBoolean(HourlyApplication.PREFERENCE_SPEAK_AMPM, false);

        String lang = shared.getString(HourlyApplication.PREFERENCE_LANGUAGE, "");

        Locale locale;

        if (lang.isEmpty())
            locale = Locale.getDefault();
        else
            locale = new Locale(lang);

        if (tts.isLanguageAvailable(locale) == TextToSpeech.LANG_NOT_SUPPORTED) {
            locale = new Locale("en_US");
        }

        String speakAMPM = "";
        String speakHour = "";
        String speakMinute = "";

        // Russian requires dots "." and hours/minutes
        Locale ru = new Locale("ru");
        if (locale.toString().startsWith(ru.toString())) {
            if (speakAMPMFlag) {
                speakAMPM = HourlyApplication.getHourString(context, ru, hour);
            }

            speakHour = HourlyApplication.getQuantityString(context, ru, R.plurals.hours, h, h);
            speakMinute = HourlyApplication.getQuantityString(context, ru, R.plurals.minutes, min, min);

            if (min != 0) {
                speak = HourlyApplication.getString(context, ru, R.string.speak_time, ". " + speakHour + ". " + speakMinute + " " + speakAMPM);
            } else {
                if (is24) {
                    speak = HourlyApplication.getString(context, ru, R.string.speak_time_24, speakHour);
                } else if (speakAMPMFlag) {
                    speak = HourlyApplication.getString(context, ru, R.string.speak_time, ". " + speakHour + ". " + speakAMPM);
                } else {
                    speak = HourlyApplication.getString(context, ru, R.string.speak_time_12, speakHour);
                }
            }
            tts.setLanguage(ru);
        }

        // english requres zero minutes
        Locale en = new Locale("en");
        if (locale.toString().startsWith(en.toString()) || speak.isEmpty()) {
            if (speakAMPMFlag) {
                speakAMPM = HourlyApplication.getHourString(context, en, hour);
            }

            speakHour = String.format("%d", h);

            if (min < 10)
                speakMinute = String.format("oh %d", min);
            else
                speakMinute = String.format("%d", min);

            if (min != 0) {
                speak = HourlyApplication.getString(context, en, R.string.speak_time, speakHour + " " + speakMinute + " " + speakAMPM);
            } else {
                if (is24) {
                    speak = HourlyApplication.getString(context, en, R.string.speak_time_24, speakHour);
                } else if (speakAMPMFlag) {
                    speak = HourlyApplication.getString(context, en, R.string.speak_time, speakHour + " " + speakAMPM);
                } else {
                    speak = HourlyApplication.getString(context, en, R.string.speak_time_12, speakHour);
                }
            }
            tts.setLanguage(en);
        }

        Log.d(TAG, speak);

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
        restart = false;
        return true;
    }

}
