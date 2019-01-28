package com.gold.booktoaudio.presenter;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

public class BookFragmentPresenter {

    private boolean isTTSModuleOn = false;
    private TextToSpeech textToSpeech;

    private TextToSpeech.OnInitListener textToSpeechListener = (int status) -> {
        if (status == TextToSpeech.SUCCESS) {
            isTTSModuleOn = true;
        } else {
            isTTSModuleOn = false;
        }
    };

    public BookFragmentPresenter(Context context){
        textToSpeech = new TextToSpeech(context,textToSpeechListener);
    }

    public interface BookFragmentViewListener {
        void onVoiceButtonTriggered();
    }

    public void stopTTSEngine(){
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }


    private void fromTextToAudio(String text, Locale locale) {

        if(isTTSModuleOn){
            textToSpeech.setLanguage(locale);
          //  textToSpeech.speak(text,TextToSpeech.QUEUE_ADD,null,"DEFAULT"); only for api 21+
            //textToSpeech.speak(text,TextToSpeech.QUEUE_ADD,new HashMap<>());
            speech(text);
        }
    }

    public void fromTextToAudio(String text) {
        fromTextToAudio(text,Locale.getDefault());
    }

    private String getFileContent(File file) {
        return "";
    }

    private void speech(String charSequence) {
        int position =0;
        int sizeOfChar= charSequence.length();
        String testStri= charSequence.substring(position,sizeOfChar);
        int next = 20;
        int pos =0;
        while(true) {
            String temp="";
            //Log.e("in loop", "" + pos);

            try {

                temp = testStri.substring(pos, next);
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, temp);
                textToSpeech.speak(temp, TextToSpeech.QUEUE_ADD, params);

                pos = pos + 20;
                next = next + 20;

            } catch (Exception e) {
                temp = testStri.substring(pos, testStri.length());
                textToSpeech.speak(temp, TextToSpeech.QUEUE_ADD, null);
                break;

            }

        }

    }

    private void splitter(){
      //  int length = textToSpeech.getMaxSpeechInputLength() - 1;
       // Iterable<String> chunks = Splitter.fixedLength(length).split(largeText);
       // Lists.newArrayList(chunks);
    }
}
