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
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import timber.log.Timber;


public class HomeActivity extends AppCompatActivity implements BookFragmentPresenter.BookFragmentViewListener{

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
    private ProgressDialog mProgressDlg;
    private boolean playerReady = false;


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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        textRecognizer = new TextRecognizer.Builder(this).build();
        bookFragmentPresenter = new BookFragmentPresenter(this);

        mTessOCR = new TessOCR(this, language);
        findViewById(R.id.btnScan).setOnClickListener((View v)-> {
                    Intent scanIntent =  new Intent(this,OpenNoteScannerActivity.class);
                    startActivityForResult(scanIntent,SCAN_CODE);
                }
        );

        mTextMessage =  findViewById(R.id.message);
        BottomNavigationView navigation =  findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        findViewById(R.id.btn).setOnClickListener((View v)-> record());
        findViewById(R.id.btnFile).setOnClickListener((View v)->{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            //intent.setType("application/pdf");
            startActivityForResult(intent, REQ_FILE_PICKER);
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


    private void doOCR (final Bitmap bitmap) {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, "Processing",
                    "Doing OCR...", true);
        } else {
            mProgressDialog.show();
        }
        new Thread(new Runnable() {
            public void run() {
                final String srcText = mTessOCR.getOCRResult(bitmap);
                HomeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if (srcText != null && !srcText.equals("")) {
                            //srcText contiene el texto reconocido

                            mTextMessage.setText(srcText);
                        }
                        mTessOCR.onDestroy();
                        mProgressDialog.dismiss();
                    }
                });
            }
        }).start();
    }


    @Override
    public void onVoiceButtonTriggered() {
        bookFragmentPresenter.fromTextToAudio(mTextMessage.getText().toString());
    }


    private void testOpenCV(){
        Mat mat = new Mat(100,100, CvType.CV_8UC4);
        Timber.d(getClass().getName()+ " width : "+mat.width());
    }



    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       // super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_FILE_PICKER:
                if (resultCode == RESULT_OK) {
                   File file  =  new File(data.getData().toString());
                    String pathHolder = FileUtils.getPath(data.getData(),getContentResolver(),this);

                    if(FileUtils.isPdfFile(pathHolder)){
                        proceedPdf(pathHolder);

                    }else{
                        //onVoiceButtonTriggered();
                        procesImageFile(data.getData());
                    }
                    Toast.makeText(HomeActivity.this, pathHolder, Toast.LENGTH_LONG).show();
                }
                break;

            case SCAN_CODE:
                if (resultCode == RESULT_OK) {

                    // processImageFile
                    String filePath = data.getStringExtra(FILE_PATH);
                    procesImageFile(filePath);
                }

                break;



        }
    }



    private void proceedPdf(String pathHolder){
        try {
            String parsedText="";
            PdfReader reader = new PdfReader(pathHolder);
            int n = reader.getNumberOfPages();
            for (int i = 0; i <n ; i++) {
                parsedText   = parsedText+ PdfTextExtractor.getTextFromPage(reader, i+1).trim()+"\n"; //Extracting the content from the different pages
            }

            onVoiceButtonTriggered();
            reader.close();
        } catch (Exception e) {
            // System.out.println(e);
            Log.e(getClass().getName(),e.getMessage());
        }
    }

    private void procesImageFile(Uri uri){
        doOCRUsingGoogleApi(FileUtils.getBitmapFromUri(uri,getContentResolver()));
    }

    private void procesImageFile(String path){

        Bitmap bitmap = FileUtils.getBitmapFromFilePath(path);
        doOCRUsingGoogleApi(bitmap);
    }


    private void doOCRUsingGoogleApi(Bitmap bitmap){

        if (!textRecognizer.isOperational()) {
            Timber.d( "Detector dependencies not loaded yet");
        } else {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();

            final SparseArray<TextBlock> items = textRecognizer.detect(frame);
            mTextMessage.post(new Runnable() {
                @Override
                public void run() {
                    StringBuilder stringBuilder = new StringBuilder();
                    for(int i=0;i<items.size();i++){
                        TextBlock item = items.valueAt(i);
                        stringBuilder.append(item.getValue());
                        stringBuilder.append("\n");
                    }
                    mTextMessage.setText(stringBuilder.toString());
                   // onVoiceButtonTriggered();
                    record();
                }
            });

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

    private void initComponent(){
        // Instantiating TextToSpeech class
        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
               // mStatus = i;
               // setTts(mTts);
            }
        });

        // Getting reference to the button btn_speek
        Button btnSpeek = (Button) findViewById(R.id.btn);

        // Creating a progress dialog window
        mProgressDialog = new ProgressDialog(this);

        // Creating an instance of MediaPlayer
        mMediaPlayer = new MediaPlayer();

        // Close the dialog window on pressing back button
        mProgressDialog.setCancelable(true);

        // Setting a horizontal style progress bar
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        /** Setting a message for this progress dialog
         * Use the method setTitle(), for setting a title
         * for the dialog window
         *  */
        mProgressDialog.setMessage("Please wait ...");

        // Defining click event listener for the button btn_speak
        View.OnClickListener btnClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if(mStatus==TextToSpeech.SUCCESS){

                    // Getting reference to the Button
                    Button btnSpeak = (Button) findViewById(R.id.btn);

                    btnSpeak.setText("Pause");

                    if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
                        playMediaPlayer(1);
                        btnSpeak.setText("Speak");
                        return;
                    }

                    mProgressDialog.show();


                    HashMap<String, String> myHashRender = new HashMap();
                    String utteranceID = "wpta";
                    myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID);

                    String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + FILENAME;

                    if(!mProcessed){
                        int status = mTts.synthesizeToFile(mTextMessage.getText().toString(), myHashRender, fileName);
                    }else{
                        playMediaPlayer(0);
                    }
                }else{
                    String msg = "TextToSpeech Engine is not initialized";
                    Toast.makeText(getBaseContext(),msg, Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Set Click event listener for the button btn_speak
        btnSpeek.setOnClickListener(btnClickListener);

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
