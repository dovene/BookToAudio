package com.gold.booktoaudio.presenter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.Toast;

import com.gold.booktoaudio.R;
import com.gold.booktoaudio.helper.FileUtils;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class BookFragmentPresenter {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    public final static int DELAY_BEFORE_EXIT = 1000; //1s
    public final static int TEXT_TO_SPEECH_SPLIT_NUMBER = 20;
    private final String FILENAME = "/wpta_tts.wav";

    private MediaPlayer mediaPlayer;

    public interface PresenterInterface {
        void onImageOCRized(String text);

        void onPdfTextExtracted(String text);
    }

    private PresenterInterface presenterInterface;

    private boolean isTTSModuleOn = false;
    private TextToSpeech textToSpeech;
    private Context context;

    private TextToSpeech.OnInitListener textToSpeechListener = (int status) -> {
        if (status == TextToSpeech.SUCCESS) {
            isTTSModuleOn = true;
            setTts(textToSpeech);
        } else {
            isTTSModuleOn = false;
        }
    };

    public BookFragmentPresenter(Context context, PresenterInterface presenterInterface) {
        this.textToSpeech = new TextToSpeech(context, textToSpeechListener);
        this.presenterInterface = presenterInterface;
        initMediaPlayer();
        this.context = context;
    }

    public interface BookFragmentViewListener {
        void onVoiceButtonTriggered();

        void onTaskStarted();

        void onTaskEnded();
    }

    public void stopTTSEngine() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }


    private void fromTextToAudio(String text, Locale locale) {

        if (isTTSModuleOn) {
            textToSpeech.setLanguage(locale);
            //  textToSpeech.speak(text,TextToSpeech.QUEUE_ADD,null,"DEFAULT"); only for api 21+
            //textToSpeech.speak(text,TextToSpeech.QUEUE_ADD,new HashMap<>());
            speech(text);
        }
    }

    public void fromTextToAudio(String text) {
        fromTextToAudio(text, Locale.getDefault());
    }

    private String getFileContent(File file) {
        return "";
    }

    private void speech(String charSequence) {
        int position = 0;
        int sizeOfChar = charSequence.length();
        String testStri = charSequence.substring(position, sizeOfChar);
        int next = TEXT_TO_SPEECH_SPLIT_NUMBER;
        int pos = 0;
        while (true) {
            String temp = "";
            //Log.e("in loop", "" + pos);

            try {

                temp = testStri.substring(pos, next);
                HashMap<String, String> params = new HashMap<String, String>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, temp);
            //    textToSpeech.speak(temp, TextToSpeech.QUEUE_ADD, params);
                //String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + FILENAME;
                //String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + FILENAME;
                //recordSpeech(temp);

                File soundFile = new File(getAudioFilePath());
                if (soundFile.exists())
                    soundFile.delete();

                if (textToSpeech.synthesizeToFile(temp, params, getAudioFilePath()) == TextToSpeech.SUCCESS) {
                    Toast.makeText(context, "Sound file created", Toast.LENGTH_SHORT).show();
                    // playSoundFile(soundFile);
                    //if(playerReady){
                    // playMediaPlayer(0);
                    //playTextWithMediaPlayer(getAudioFilePath());

                } else {
                    Toast.makeText(context, "Oops! Sound file not created", Toast.LENGTH_SHORT).show();
                }




                pos = pos + TEXT_TO_SPEECH_SPLIT_NUMBER;
                next = next + TEXT_TO_SPEECH_SPLIT_NUMBER;

            } catch (Exception e) {
                temp = testStri.substring(pos, testStri.length());
                textToSpeech.speak(temp, TextToSpeech.QUEUE_ADD, null);
                break;

            }

        }

    }

    private void recordSpeech(String text) {

        HashMap<String, String> myHashRender = new HashMap<>();
        String utteranceID = "wpta";
        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID);


        File soundFile = new File(getAudioFilePath());
        if (soundFile.exists())
            soundFile.delete();

        if (textToSpeech.synthesizeToFile(text, myHashRender, getAudioFilePath()) == TextToSpeech.SUCCESS) {
            Toast.makeText(context, "Sound file created", Toast.LENGTH_SHORT).show();
            // playSoundFile(soundFile);
            //if(playerReady){
            // playMediaPlayer(0);
            //playTextWithMediaPlayer(getAudioFilePath());

        } else {
            Toast.makeText(context, "Oops! Sound file not created", Toast.LENGTH_SHORT).show();
        }
    }


    private Observable<String> getTextFromFileObservable(String path, TextRecognizer textRecognizer) {
        String result = null;
        Bitmap bitmap = FileUtils.getBitmapFromFilePath(path);
        if (textRecognizer.isOperational()) {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();

            final SparseArray<TextBlock> items = textRecognizer.detect(frame);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                TextBlock item = items.valueAt(i);
                stringBuilder.append(item.getValue());
                stringBuilder.append("\n");
            }
            // mTextMessage.setText(stringBuilder.toString());
            result = stringBuilder.toString();

        }
        return Observable.just(result);
    }

    public DisposableObserver<String> getTextFromFileObserver(String path, TextRecognizer textRecognizer) {
        DisposableObserver<String> disposableObserver = getTextFromFileObservable(path, textRecognizer)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    public void onNext(String text) {
                        //record(booleanStringHashMap.get(true));
                        presenterInterface.onImageOCRized(text);

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        compositeDisposable.add(disposableObserver);
        return disposableObserver;

    }

    public void onUIDestroy() {
        compositeDisposable.clear();
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
        if( textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }


    private Observable<String> extractTextFromPdfObservable(String pathHolder) {
        String result = "";
        try {
            String parsedText = "";
            PdfReader reader = new PdfReader(pathHolder);
            int n = reader.getNumberOfPages();
            for (int i = 0; i < n; i++) {
                parsedText = parsedText + PdfTextExtractor.getTextFromPage(reader, i + 1).trim() + "\n"; //Extracting the content from the different pages
            }

            result = parsedText;
            reader.close();
        } catch (Exception e) {
            Timber.e(e);
        }
        return Observable.just(result);

    }

    public DisposableObserver<String> extractTextFromPdfObserver(String path) {
        DisposableObserver<String> observer = extractTextFromPdfObservable(path)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    public void onNext(String s) {
                        presenterInterface.onPdfTextExtracted(s);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        compositeDisposable.add(observer);
        return observer;
    }


    private synchronized void playMediaPlayer() {
        //  int max = TextToSpeech.getMaxSpeechInputLength();

        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }else{
            mediaPlayer.start();
        }

    }

    private void initializeMediaPlayer() {
        Uri uri = Uri.parse("file://" + getAudioFilePath());

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.prepare();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void initMediaPlayer(){

        mediaPlayer = new MediaPlayer();
        MediaPlayer.OnCompletionListener mediaPlayerCompletionListener = new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {

            }
        };

        mediaPlayer.setOnCompletionListener(mediaPlayerCompletionListener);
    }

    @NonNull
    private String getAudioFilePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + FILENAME;
    }


    @SuppressWarnings("deprecation")
    @TargetApi(15)
    public void setTts(TextToSpeech tts) {
        this.textToSpeech = tts;

        if( Build.VERSION.SDK_INT  >= 15 ){
            this.textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId){
                    // Speech file is created
                   // mProcessed = true;

                    // Initializes Media Player
                    initializeMediaPlayer();

                    // Start Playing Speech
                    playMediaPlayer();
                }

                @Override
                public void onError(String utteranceId){
                }

                @Override
                public void onStart(String utteranceId){
                }
            });
        }else{
            this.textToSpeech.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    // Speech file is created


                    // Initializes Media Player
                    initializeMediaPlayer();

                    // Start Playing Speech
                    playMediaPlayer();
                }
            });
        }
    }

}
