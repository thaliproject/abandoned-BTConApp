// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btconapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class MyTextSpeech implements TextToSpeech.OnInitListener {
    private TextToSpeech _tts;


    public MyTextSpeech(Context context) {
        _tts = new TextToSpeech(context, this);
    }


    public void stop() {
        if (_tts != null) {
            _tts.stop();
            _tts.shutdown();
        }
    }

    @Override
    public void onInit(final int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = _tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                //
            }
            else {
                String msg = "hi there, i'm ready";
                speak(msg);
            }
        }
    }
    public void speak(final String text) {
        _tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}
