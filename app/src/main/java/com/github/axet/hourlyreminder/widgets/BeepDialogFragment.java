package com.github.axet.hourlyreminder.widgets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.PreferenceDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.axet.hourlyreminder.R;
import com.github.axet.hourlyreminder.app.Sound;
import com.github.axet.hourlyreminder.basics.Reminder;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class BeepDialogFragment extends PreferenceDialogFragment {
    private boolean mPreferenceChanged;

    Sound sound;

    TextView len;
    TextView freq;
    SeekBar seek;
    boolean ignore = false;

    int fmin = 20;
    int fmax = 20000;

    public static class BeepConfig {
        public int value_f;
        public int value_l;

        public void reset() {
            value_f = 1800;
            value_l = 100;
        }

        public void load(String values) {
            reset();

            String[] v = values.split(":");
            if (v.length > 0) {
                try {
                    value_f = Integer.parseInt(v[0]);
                } catch (NumberFormatException e) {
                }
            }
            if (v.length > 1) {
                try {
                    value_l = Integer.parseInt(v[1]);
                } catch (NumberFormatException e) {
                }
            }
        }

        public String save() {
            return value_f + ":" + value_l;
        }
    }

    BeepConfig beep = new BeepConfig();

    public BeepDialogFragment() {
    }

    public static BeepDialogFragment newInstance(String key) {
        BeepDialogFragment fragment = new BeepDialogFragment();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sound = new Sound(getActivity());

        if (savedInstanceState != null) {
            String values = savedInstanceState.getString("values");
            beep.load(values);
            mPreferenceChanged = savedInstanceState.getBoolean("changed");
        } else {
            BeepPreference preference = (BeepPreference) getPreference();
            String values = preference.getValues();
            beep.load(values);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("values", beep.save());
        outState.putBoolean("changed", mPreferenceChanged);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        Context context = builder.getContext();

        final BeepPreference preference = (BeepPreference) getPreference();

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.beep, null, false);
        freq = (TextView) view.findViewById(R.id.beep_freq);
        len = (TextView) view.findViewById(R.id.beep_length);
        seek = (SeekBar) view.findViewById(R.id.beep_seekbar);
        final BeepView beepView = (BeepView) view.findViewById(R.id.beep_view);
        View reset = view.findViewById(R.id.beep_reset);

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beep.reset();

                update();
            }
        });

        len.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int l = getLen();

                if (l > 5000) {
                    s.clear();
                    s.append("" + 5000);
                    l = 5000;
                }

                beep.value_l = l;
                mPreferenceChanged = true;
            }
        });

        freq.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (ignore)
                    return;

                beep.value_f = getFreq();
                mPreferenceChanged = true;

                int p = f2p(beep.value_f);
                if (p > fmax) {
                    s.clear();
                    s.append("" + fmax);
                    p = fmax;
                }
                ignore = true;
                seek.setProgress(p);
                ignore = false;
            }
        });

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int f = p2f(progress);
                beepView.setCoeff(f / 400f);
                if (ignore)
                    return;

                beep.value_f = p2f(progress);
                mPreferenceChanged = true;

                ignore = true;
                BeepDialogFragment.this.freq.setText("" + beep.value_f);
                ignore = false;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        update();

        builder.setView(view);

        builder.setNeutralButton(R.string.play, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    void update() {
        freq.setText("" + beep.value_f);
        len.setText("" + beep.value_l);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog d = (AlertDialog) super.onCreateDialog(savedInstanceState);
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = d.getButton(AlertDialog.BUTTON_NEUTRAL);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playSound();
                    }
                });
            }
        });
        return d;
    }

    // progress2freq
    int p2f(int progress) {
        int max = 100 + 1;

        float log1 = (float) (Math.log(max - progress) / Math.log(max));
        float v = 1 - log1;
        return (int) (fmin + v * (fmax - fmin));
    }

    // freq2progress
    int f2p(int freq) {
        int max = 100 + 1;

        float p = (freq - fmin) / (float) fmax;
        float log1 = 1 - p;
        int mp = (int) Math.exp(log1 * Math.log(max)); // max - progress
        int progress = max - mp;
        return progress;
    }


    void playSound() {
        int f = getFreq();
        int l = Integer.parseInt(len.getText().toString());
        sound.playBeep(Sound.generateTone(f, l), null);
    }

    int getFreq() {
        String t = freq.getText().toString();
        if (t.isEmpty())
            t = "20";
        return Integer.parseInt(t);
    }

    int getLen() {
        String t = len.getText().toString();
        if (t.isEmpty())
            t = "50";
        return Integer.parseInt(t);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        BeepPreference preference = (BeepPreference) getPreference();
        if (positiveResult && this.mPreferenceChanged) {
            if (preference.callChangeListener(null)) {
                preference.setValues(beep.save());
            }
        }

        sound.close();

        this.mPreferenceChanged = false;
    }
}
