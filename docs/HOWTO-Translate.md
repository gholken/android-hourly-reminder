# How To Translate

To translate 'Hourly Reminder' to your language you need to translate following files:

  * [pref_settings.xml](/app/src/main/res/xml/pref_settings.xml)
  * [pref_reminders.xml](/app/src/main/res/xml/pref_reminders.xml)
  * [strings.xml](/app/src/main/res/values/strings.xml)

Additional file from 'android-library'
  * https://github.com/axet/android-library/blob/master/src/main/res/values/strings.xml

In additional, you need to figure it out, which is the best way to pronounce current time. For example you may want to pronounce "hours" after hours and "minutes" after minutes. Or just prounouce time as it is. You can find how speach time string builded here:

  * [TTS.java](/app/src/main/java/com/github/axet/hourlyreminder/app/TTS.java)

```java
            if (speakAMPMFlag) { // do we need to prounounce AM/PM?
                speakAMPM = HourlyApplication.getHourString(context, en, hour);
            }

            // we can use "10 hours" or just "10"
            speakHour = HourlyApplication.getQuantityString(context, en, R.plurals.hours, h);
            
            if (min < 10) // in case we need to say 10 hours, 08 minutes : 10 "o" 8
                speakMinute = String.format("o %d", min);
            else
                speakMinute = HourlyApplication.getQuantityString(context,
                  en,
                  R.plurals.minutes, min);

            if (min != 0) {
                speak = HourlyApplication.getString(context,
                  en,
                  R.string.speak_time,
                  speakHour + " " + speakMinute + " " + speakAMPM);
            } else {
                if (speakAMPMFlag)
                    speak = HourlyApplication.getString(context,
                      en,
                      R.string.speak_time,
                      speakHour + " " + speakAMPM);
                else
                    speak = HourlyApplication.getString(context,
                      en,
                      R.string.speak_time_24,
                      speakHour);
            }
            tts.setLanguage(en);
```

Then add those files and speak engine to the repository using "New Issue" or create pull request.
