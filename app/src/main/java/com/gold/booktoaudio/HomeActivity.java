package com.gold.booktoaudio;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.gold.booktoaudio.helper.FileUtils;
import com.gold.booktoaudio.helper.TessOCR;
import com.gold.booktoaudio.presenter.BookFragmentPresenter;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.util.HashMap;

import booktoaudiio.gold.com.imagescanner.OpenNoteScannerActivity;
import static booktoaudiio.gold.com.imagescanner.OpenNoteScannerActivity.FILE_PATH;
import static com.gold.booktoaudio.presenter.BookFragmentPresenter.DELAY_BEFORE_EXIT;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class HomeActivity extends AppCompatActivity implements BookFragmentPresenter.BookFragmentViewListener,
        BookFragmentPresenter.PresenterInterface{

    private EditText mTextMessage;
    private BookFragmentPresenter bookFragmentPresenter;
    public final static int REQ_FILE_PICKER=1;
    private TessOCR  mTessOCR;
    private String language= "fra";//"eng";
    private ProgressDialog mProgressDialog;
    private final static int SCAN_CODE = 12;

    private TextRecognizer textRecognizer;

    private TextToSpeech mTts;
    private int mStatus = 0;
    private MediaPlayer mMediaPlayer;
    private boolean mProcessed = false;
    private final String FILENAME = "/wpta_tts.wav";


    private ProgressBar progressBar;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onImageOCRized(String text) {
        //record(text);
        mTextMessage.setText(text);
        onTaskEnded();
    }



    private ProgressBar getProgressBar(){
        if (progressBar==null){
            progressBar = new ProgressBar(this,null,android.R.attr.progressBarStyle);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100,100);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            ((LinearLayout)findViewById(R.id.content)).addView(progressBar,params);
            progressBar.setIndeterminate(true);
            getProgressBar().setVisibility(View.GONE);
        }
        return progressBar;
    }

    @Override
    public void onTaskStarted() {

            getProgressBar().setVisibility(View.VISIBLE);


    }

    @Override
    public void onTaskEnded() {
        getProgressBar().setVisibility(View.GONE);
    }

    @Override
    public void onPdfTextExtracted(String text) {
        mTextMessage.setText(text);
        onTaskEnded();
    }

    private TextRecognizer getTextRecognizer(){
        if(textRecognizer==null){
            textRecognizer = new TextRecognizer.Builder(this).build();
        }
       return textRecognizer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bookFragmentPresenter = new BookFragmentPresenter(this, this);


        //mTessOCR = new TessOCR(this, language);
        findViewById(R.id.btnScan).setOnClickListener((View v)-> {
                    Intent scanIntent =  new Intent(this,OpenNoteScannerActivity.class);
                    startActivityForResult(scanIntent,SCAN_CODE);
                    onTaskStarted();
                }
        );


        mTextMessage =  findViewById(R.id.message);
        BottomNavigationView navigation =  findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        findViewById(R.id.btn).setOnClickListener((View v)->
                onVoiceButtonTriggered()
                //record()
                );
        findViewById(R.id.btnFile).setOnClickListener((View v)->{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            //intent.setType("application/pdf");
            startActivityForResult(intent, REQ_FILE_PICKER);
            onTaskStarted();
        });
        initMediaPlayer();
        // initComponent();
       // Timber.d("ninja : "+String.valueOf(OpenCVLoader.initDebug()));
        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                 mStatus = i;
                 setTts(mTts);
                 initMediaPlayer();
            }
        });
    }

    private void initMediaPlayer(){

        mMediaPlayer = new MediaPlayer();
        MediaPlayer.OnCompletionListener mediaPlayerCompletionListener = new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // Getting reference to the button btn_speek
                Button btnSpeek = (Button) findViewById(R.id.btn);

                // Changing button Text to Speek
                btnSpeek.setText("Speak");
            }
        };

        mMediaPlayer.setOnCompletionListener(mediaPlayerCompletionListener);
    }


    @Override
    public void onVoiceButtonTriggered() {
        bookFragmentPresenter.fromTextToAudio(mTextMessage.getText().toString());
    }

    private  void processResult(int requestCode, int resultCode, Intent data){
        onTaskStarted();
        switch (requestCode) {
            case REQ_FILE_PICKER:
                if (resultCode == RESULT_OK) {
                    processSelecteFile(data.getData());
                    //Toast.makeText(HomeActivity.this, pathHolder, Toast.LENGTH_LONG).show();
                }
                break;

            case SCAN_CODE:
                if (resultCode == RESULT_OK) {
                    if (!TextUtils.isEmpty(data.getStringExtra(FILE_PATH))){
                        bookFragmentPresenter.getTextFromFileObserver(data.getStringExtra(FILE_PATH),getTextRecognizer());
                    }
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       // super.onActivityResult(requestCode, resultCode, data);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processResult(requestCode,resultCode,data);
            }
        },DELAY_BEFORE_EXIT);
    }

    private void  processSelecteFile(Uri uri){
        File file  =  new File(uri.toString());
        String pathHolder = FileUtils.getPath(uri,getContentResolver(),this);
        if(FileUtils.isPdfFile(pathHolder)){
            if (!TextUtils.isEmpty(pathHolder)){
                bookFragmentPresenter.extractTextFromPdfObserver(pathHolder);
            }
        }else{
            if (!TextUtils.isEmpty(uri.toString())){
                bookFragmentPresenter.getTextFromFileObserver(FileUtils.getPath(uri,getContentResolver(),this),getTextRecognizer());
            }
        }
    }


    @SuppressWarnings("deprecation")
    @TargetApi(15)
    public void setTts(TextToSpeech tts) {
        this.mTts = tts;

        if( Build.VERSION.SDK_INT  >= 15 ){
            this.mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId){
                    // Speech file is created
                    mProcessed = true;

                    // Initializes Media Player
                    initializeMediaPlayer();

                    // Start Playing Speech
                    playMediaPlayer(0);
                }

                @Override
                public void onError(String utteranceId){
                }

                @Override
                public void onStart(String utteranceId){
                }
            });
        }else{
            this.mTts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    // Speech file is created
                    mProcessed = true;

                    // Initializes Media Player
                    initializeMediaPlayer();

                    // Start Playing Speech
                    playMediaPlayer(0);
                }
            });
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        if( mTts != null)
            mTts.stop();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        if( mTts != null) {
            mTts.shutdown();
        }
        bookFragmentPresenter.onUIDestroy();
    }


    private void freeResources(){
        // Stop the TextToSpeech Engine
        mTts.stop();

        // Shutdown the TextToSpeech Engine
        mTts.shutdown();

        // Stop the MediaPlayer
        mMediaPlayer.stop();

        // Release the MediaPlayer
        mMediaPlayer.release();

    }

    private void initializeMediaPlayer(){
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + FILENAME;

        Uri uri  = Uri.parse("file://"+fileName);

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(getApplicationContext(), uri);
            mMediaPlayer.prepare();
           /* mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    playerReady=true;
                }
            });
            mMediaPlayer.prepareAsync();*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playMediaPlayer(int status){
        /*mProgressDialog.dismiss();

        // Start Playing
        if(status==0){
            mMediaPlayer.start();
        }

        // Pause Playing
        if(status==1){
            mMediaPlayer.pause();
        }*/

        if(mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
        }else{
            mMediaPlayer.start();
        }
    }


    private void record(){

        HashMap<String, String> myHashRender = new HashMap();
        String utteranceID = "wpta";
        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID);

        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + FILENAME;

        File soundFile = new File(fileName);
        if (soundFile.exists())
            soundFile.delete();

        if(mTts.synthesizeToFile(mTextMessage.getText().toString(), myHashRender, fileName)== TextToSpeech.SUCCESS) {
            Toast.makeText(getBaseContext(),"Sound file created",Toast.LENGTH_SHORT).show();
           // playSoundFile(soundFile);
            //if(playerReady){
                playMediaPlayer(0);
           // }

        } else {
            Toast.makeText(getBaseContext(),"Oops! Sound file not created",Toast.LENGTH_SHORT).show();
        }
    }

    private void playSoundFile (File soundFilename){
        Timber.d("soundFilename "+soundFilename);
        Uri uri  = Uri.parse("file://"+soundFilename);
        mMediaPlayer =  new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        try {

            mMediaPlayer.setDataSource(getApplicationContext(),uri);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        }
        catch(Exception e) {
            Toast.makeText(getBaseContext(),"Hmmmmm. Can't play file",Toast.LENGTH_SHORT).show();
          Timber.e(e);
        }
    }



}
