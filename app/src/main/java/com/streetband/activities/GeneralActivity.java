package com.streetband.activities;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.streetband.R;
import com.streetband.customViews.CustomCountdown;
import com.streetband.customViews.CustomSeekBar;
import com.streetband.fragments.ChineseDrumsKitFragment;
import com.streetband.fragments.GrandPianoFragment;
import com.streetband.fragments.MainBoardFragment;
import com.streetband.fragments.SettingsFragment;
import com.streetband.managers.InstrumentManager;
import com.streetband.managers.SettingsManager;
import com.streetband.models.ChineseDrumsKit;
import com.streetband.models.GrandPiano;
import com.streetband.models.Instrument;
import com.streetband.threads.PlayerManager;

import java.io.IOException;

public class GeneralActivity extends AppCompatActivity {
    private static final String TAG = "GeneralActivity";
    private static final String METRONOME_FOLDER = "metronome";
    public static final String SYSTEM_SOUNDS_FOLDER = "systemSounds";
    public static final int DEFAULT_SONG_LENGTH = 16;
    public static final int DEFAULT_SONG_TACT = 120;

    //views
    private ImageButton mAddInstrumentButton;
    private ImageButton mMainBoardButton;
    private ImageButton mStopButton;
    private ImageButton mPlayButton;
    private ImageButton mRecordButton;
    private CheckBox mMetronomeBox;
    private Button mDoneButton;
    private ImageView mSettingsView;

    //custom views
    private CustomCountdown mCountdown;
    private CustomSeekBar mCustomSeekBar;

    //listeners
    private RecordListener mRecordListener;

    //tools
    private SoundPool mMetronomeSoundPool;

    //player
    private PlayerManager mPlayerManager;

    //managers
    private AssetManager mAssetManager;
    private FragmentManager mFragmentManager;
    private InstrumentManager mInstrumentManager;
    private SettingsManager mSettingsManger;

    //fragments
    private MainBoardFragment mMainBoardFragment;
    private SettingsFragment mSettingsFragment;


    //dynamic params
    private int mMetronomeBigId;
    private int mMetronomeId;
    private int mStartSoundId;

    private boolean isPlaying;
    private boolean isPaused;
    private boolean isInSettings;
    private boolean isRowOpened;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general);
        //todo
        //view binding
        mAddInstrumentButton = findViewById(R.id.toolbar_add_instrument);
        mMainBoardButton = findViewById(R.id.toolbar_main_board);
        mStopButton = findViewById(R.id.toolbar_stop);
        mPlayButton = findViewById(R.id.toolbar_play);
        mRecordButton = findViewById(R.id.toolbar_record);
        mMetronomeBox = findViewById(R.id.toolbar_metronome);
        mDoneButton = findViewById(R.id.toolbar_done);
        mSettingsView = findViewById(R.id.toolbar_settings);

        mCountdown = findViewById(R.id.main_countdown);
        mCustomSeekBar = findViewById(R.id.toolbar_customSeekBar);


        //tools
        mMetronomeSoundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();

        //managers
        mSettingsManger = SettingsManager.getInstance();
        mSettingsManger.setSongLength(DEFAULT_SONG_LENGTH);
        mCustomSeekBar.setLength(mSettingsManger.getSongLength());
        mSettingsManger.setTact(DEFAULT_SONG_TACT);
        mSettingsManger.addSettingsManagerListener(new SettingsManager.SettingsManagerListener() {
            @Override
            public void songLengthChanged(int songLength) {
                mCustomSeekBar.setLength(songLength);
            }

            @Override
            public void tactChanged(int tact) {
            }
        });

        mFragmentManager = getSupportFragmentManager();
        mMainBoardFragment = new MainBoardFragment();
        mFragmentManager.beginTransaction().add(R.id.main_container,mMainBoardFragment).commit();
        mInstrumentManager = InstrumentManager.getInstance();
        mAssetManager = getAssets();
        try{
            AssetFileDescriptor assetFileDescriptor = mAssetManager.openFd(METRONOME_FOLDER + "/metronome_big.wav");
            mMetronomeBigId = mMetronomeSoundPool.load(assetFileDescriptor,0);
            assetFileDescriptor = mAssetManager.openFd(METRONOME_FOLDER + "/metronome_small.wav");
            mMetronomeId = mMetronomeSoundPool.load(assetFileDescriptor,0);
            assetFileDescriptor = mAssetManager.openFd(SYSTEM_SOUNDS_FOLDER + "/start_sound.wav");
            mStartSoundId = mMetronomeSoundPool.load(assetFileDescriptor,1);
        }catch (IOException e){
            e.getStackTrace();
        }



        //View clicks
        mAddInstrumentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GeneralActivity.this,InstrumentsActivity.class);
                startActivity(intent);
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPlaying) {
                    mPlayerManager.onStop();
                }
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    if(isPaused){
                        mPlayerManager.onResume();
                    }else {
                        mPlayerManager.onPause();
                    }
                    isPaused = !isPaused;
                    mPlayButton.setSelected(!mPlayButton.isSelected());
                } else {
                    mPlayerManager = new PlayerManager(GeneralActivity.this);
                    mPlayerManager.start();
                    mPlayButton.setBackgroundColor(Color.GREEN);
                    mPlayButton.setSelected(true);
                    new Cursor().execute(false);
                }
            }
        });

        mRecordButton.setEnabled(false);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCountdown.setVisibility(View.VISIBLE);
                mRecordButton.setBackgroundColor(Color.RED);
                mPlayButton.setBackgroundColor(Color.GREEN);
                new PreRecorder().execute();
            }
        });

        mSettingsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isInSettings) {
                    mSettingsFragment = new SettingsFragment();
                    mFragmentManager.beginTransaction().add(R.id.main_container_2, mSettingsFragment).commit();
                    mDoneButton.setVisibility(View.VISIBLE);
                    isInSettings = true;
                }
            }
        });

        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isInSettings){
                    isInSettings = false;
                    mSettingsFragment.publishUpdates();
                    mFragmentManager.beginTransaction().remove(mFragmentManager.findFragmentById(R.id.main_container_2)).commit();
                }else {
                    isRowOpened = false;
                    mMainBoardFragment.closeRow();
                }
                if(!isInSettings && !isRowOpened){
                    mDoneButton.setVisibility(View.GONE);
                }
            }
        });
        mMainBoardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecordButton.setEnabled(false);
                mMainBoardFragment = new MainBoardFragment();
                mFragmentManager.beginTransaction().setCustomAnimations(R.animator.scale_in_animator,R.animator.scale_out_animator)
                        .replace(R.id.main_container,mMainBoardFragment).commit();
                mMainBoardButton.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMetronomeSoundPool.play(mStartSoundId,1.0f,1.0f,1,0,0);
        mMetronomeSoundPool.unload(mStartSoundId);
    }

    public void editBoardOpened(){
        mDoneButton.setVisibility(View.VISIBLE);
        isRowOpened = true;
    }

    public CustomSeekBar getSeekBar(){
        return mCustomSeekBar;
    }


    public void instrumentSelected(Instrument instrument){
        Fragment fragment;
        if(instrument.getName().equals(getString(R.string.grand_piano))){
            fragment = GrandPianoFragment.newInstance((GrandPiano)instrument);
            mRecordListener = (GrandPianoFragment)fragment;
        }else{
            fragment = ChineseDrumsKitFragment.newInstance((ChineseDrumsKit)instrument);
            mRecordListener = (ChineseDrumsKitFragment)fragment;
        }
        ObjectAnimator.ofInt(mCustomSeekBar, "left", mCustomSeekBar.getLeft(), 0).setDuration(1000).start();
        mMainBoardFragment = null;
        mMainBoardButton.setVisibility(View.VISIBLE);
        mFragmentManager.beginTransaction().setCustomAnimations(R.animator.scale_in_animator, R.animator.scale_out_animator)
                .replace(R.id.main_container, fragment).commit();
        mRecordButton.setEnabled(true);

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(GeneralActivity.this).setTitle(R.string.warning_dialog_title)
                .setMessage(R.string.warning_dialog_message)
                .setPositiveButton(R.string.warning_dialog_Positive_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //TODO
                    }
                })
                .setNegativeButton(R.string.warning_dialog_negative_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        GeneralActivity.this.onBackPressed();
                    }
                }).create().show();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///INNER CLASSES
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class PreRecorder extends android.os.AsyncTask<Void,Integer,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRecordListener.prepareRecording();
            mCustomSeekBar.setPosition(0);
            mCustomSeekBar.setIsRecording(true);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int i = 0;
            while (i < 4){
                publishProgress(i);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i+= 1;
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int i = values[0];
            if(i%4 == 0){
                mCountdown.setSelectedNumber(i + 1);
                mMetronomeSoundPool.play(mMetronomeBigId,1.0f,1.0f,0,0,0);
            }else {
                mCountdown.setSelectedNumber(i + 1);
                mMetronomeSoundPool.play(mMetronomeId,1.0f,1.0f,0,0,0);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mCountdown.setVisibility(View.GONE);
            mMetronomeSoundPool.play(mMetronomeBigId,1.0f,1.0f,0,0,0);
            mRecordListener.startRecording();
            new Cursor().execute(true);
//            mPlayerManager = new PlayerManager(GeneralActivity.this);
//            mPlayerManager.start();
        }
    }


    //Cursor mover
    private class Cursor extends AsyncTask<Boolean, Void, Void> {
        private long mStartTime;
        private long mEndTime;
        private float mPaddingInTime;
        private float mCurrentPositionInTact;

        private boolean isRecording;

        @Override
        protected void onPreExecute() {
            isPlaying = true;
            mStartTime = System.currentTimeMillis();
            mEndTime = (long)(4*mSettingsManger.getSongLength()*1000/((float)mSettingsManger.getTact()/60)) + mStartTime;

            mPaddingInTime = 4 * 60 / mSettingsManger.getTact() * 1000;
        }

        @Override
        protected Void doInBackground(Boolean... booleans) {
            isRecording = booleans[0];
            while (mEndTime > System.currentTimeMillis()){
                if (!isPaused) {
                    mCurrentPositionInTact = (System.currentTimeMillis() - mStartTime) / mPaddingInTime;
                    publishProgress();
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    mEndTime += (System.currentTimeMillis() - mEndTime);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            mCustomSeekBar.setPosition(mCurrentPositionInTact);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            isPlaying = false;
            mPlayButton.setBackgroundResource(R.drawable.round_rect_selector);
            mPlayButton.setSelected(false);
            mCustomSeekBar.setPosition(0);
            if (isRecording) {
                mRecordListener.finishRecording();
                mRecordButton.setBackgroundResource(R.drawable.round_rect_selector);
                mCustomSeekBar.setIsRecording(false);
            }else {
                mPlayerManager.quit();
            }
        }
    }


    public interface RecordListener{
        void prepareRecording();
        void startRecording();
        void stopRecording();
        void finishRecording();
    }
}
