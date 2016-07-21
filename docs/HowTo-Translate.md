# How To Translate

To translate 'Hourly Reminder' to your language you need to translate following files:

  * [pref_settings.xml](/app/src/main/res/xml/pref_settings.xml)
  * [pref_reminders.xml](/app/src/main/res/xml/pref_reminders.xml)
  * [strings.xml](/app/src/main/res/values/strings.xml)

In additional, you need to figure it out, which is the best way to pronounce current time. For example you may want to pronounce "hours" after hours and "minutes" after minutes. Or just prounouce time as it is. You can find how speach time string builded here:

  * [TTS.java](/app/src/main/java/com/github/axet/hourlyreminder/app/TTS.java)

