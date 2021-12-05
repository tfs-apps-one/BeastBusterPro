package tfsapps.beastbusterpro;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
//タイマースレッド
import java.io.IOException;
import java.security.Policy;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
//アラーム関連
import android.media.MediaPlayer;
import android.media.AudioManager;
// ライト
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
//  加速度センサ
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

//国関連
import java.util.Locale;

import static java.lang.Thread.sleep;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //  DB関連
    private MyOpenHelper helper;    //DBアクセス
    private int db_isopen = 0;      //DB使用したか
    private int db_normal = 0;      //DB通常音タイプ
    private int db_emergency = 0;   //DB緊急音タイプ
    private int db_interval = 0;    //DB連続再生間隔
    private int db_volume1 = 0;     //DB音量（通常音）
    private int db_volume2 = 0;     //DB音量（緊急音）
    private int db_light1 = 0;      //DBライト（通常音）
    private int db_light2 = 0;      //DBライト（緊急音）
    private int db_shake = 0;       //DB振る

    private Spinner sp_sound1;      //通常音選択
    private Spinner sp_sound2;      //SOS音選択
    private Spinner sp_light1;      //通常ライト選択
    private Spinner sp_light2;      //SOSライト選択
    private Spinner sp_interval;    //再生間隔

    private SeekBar seek_volume1;    //通常音量
    private SeekBar seek_volume2;    //SOS音量

    private ToggleButton toggle_normal;      //通常音状態（ON/OFF）
    private ToggleButton toggle_emergency;   //異常音状態（ON/OFF）
    private Switch sw_shake;                 //振る振る

    //  国設定
    private Locale _local;
    private String _language;
    private String _country;

    //  効果音
    private AudioManager am;
    private MediaPlayer bgm;
    private MediaPlayer countText;			    //テキストビュー
    private int bak_select;
    private int start_volume;
    private int set_interval;
    //  スレッド
    private boolean blinking = false;
    private Timer mainTimer1;					//タイマー用
    private MainTimerTask mainTimerTask1;		//タイマタスククラス
    private Timer mainTimer2;					//タイマー用
    private MainTimerTask mainTimerTask2;		//タイマタスククラス
    private Timer mainTimer3;					//タイマー用
    private MainTimerTask mainTimerTask3;		//タイマタスククラス
    private Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ
    private Timer emerTimer;					//タイマー用
    private EmerTimerTask emerTimerTask;		//タイマタスククラス
    private Handler eHandler = new Handler();   //UI Threadへのpost用ハンドラ
    private Timer blinkTimer;					//タイマー用
    private BlinkingTask blinkTimerTask;		//タイマタスククラス
    private Handler bHandler = new Handler();   //UI Threadへのpost用ハンドラ

    //  ライト関連
    private CameraManager mCameraManager;
    private String mCameraId = null;
    private boolean isOn = false;
    protected final static double RAD2DEG = 180/Math.PI;
    SensorManager sensorManager;
    float[] rotationMatrix = new float[9];
    float[] gravity = new float[3];
    float[] geomagnetic = new float[3];
    float[] attitude = new float[3];
    private boolean emergency_playing = false;
    private int roll_plus = 0;
    private int roll_minus = 0;
    private int pitch_zero = 0;
    private int sec_five = 0;

    final private int INTERVAL_0 = 0;
    final private int INTERVAL_1 = 1000;
    final private int INTERVAL_3 = 3000;
    final private int INTERVAL_5 = 5000;
    final private int INTERVAL_7 = 7000;
    final private int INTERVAL_10 = 10000;
    final private int INTERVAL_15 = 15000;
    final private int INTERVAL_20 = 20000;
    final private int INTERVAL_30 = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  国設定
        _local = Locale.getDefault();
        _language = _local.getLanguage();
        _country = _local.getCountry();

        //  音量
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        start_volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);

        //カメラ初期化
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                super.onTorchModeChanged(cameraId, enabled);
                mCameraId = cameraId;
                isOn = enabled;
            }
        }, new Handler());

        //  ボタン処理
        seekSelect();
        spinnerSelect();
        toggleSelect();
    }
    /* **************************************************
        各種OS上の動作定義
    ****************************************************/
    @Override
    public void onStart() {
        super.onStart();

        //DBのロード
        /* データベース */
        helper = new MyOpenHelper(this);
        AppDBInitRoad();
        screen_display();

        //センサ初期化
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }
        //音声初期化
        if (am == null) {
            am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }

        //センサ監視起動
        this.emerTimer = new Timer();
        //タスククラスインスタンス生成
        this.emerTimerTask = new EmerTimerTask();
        //タイマースケジュール設定＆開始
        this.emerTimer.schedule(emerTimerTask, 500, 1000);
    }
    @Override
    public void onResume() {
        super.onResume();
        //センサ関連
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause(){
        super.onPause();
        //  DB更新
        AppDBUpdated();
    }
    @Override
    public void onStop(){
        super.onStop();
        //  DB更新
        AppDBUpdated();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        //  DB更新
        AppDBUpdated();

        //カメラ
        if (mCameraManager != null) {
            mCameraManager = null;
        }

        /* 音量の戻しの処理 */
        if (am != null) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, start_volume, 0);
            am = null;
        }
    }

    //  戻るボタン
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンの処理
            // ダイアログ表示など特定の処理を行いたい場合はここに記述
            // 親クラスのdispatchKeyEvent()を呼び出さずにtrueを返す
            if (soundIsPlaying() == false) {
                /* そのまま終了へ */
            }
            else {
                AlertDialog.Builder ad = new AlertDialog.Builder(this);
                if (_language.equals("ja")) {
                    ad.setTitle("[戻る]は操作無効です");
                    ad.setMessage("\n\n再生を停止した後\n操作が有効になります\n\n\n\n\n");
                } else {
                    ad.setTitle("Invalid operation");
                    ad.setMessage("\n\nAfter stopping playback.\nThe operation will be effective.\n\n\n\n\n");
                }
                ad.setPositiveButton("ＯＫ", null);
                ad.show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /* **************************************************
        表示処理
    ****************************************************/
    public void screen_display(){
        /* SEEK */
        if (seek_volume1 == null) {
            seek_volume1 = (SeekBar) findViewById(R.id.seek_volume1);
        }
        seek_volume1.setProgress(db_volume1);

        if (seek_volume2 == null) {
            seek_volume2 = (SeekBar) findViewById(R.id.seek_volume2);
        }
        seek_volume2.setProgress(db_volume2);

        /* SPINNER */
        if (sp_sound1 == null) {
            sp_sound1 = (Spinner) findViewById(R.id.sp_sound1);
        }
        sp_sound1.setSelection(db_normal);

        if (sp_sound2 == null) {
            sp_sound2 = (Spinner) findViewById(R.id.sp_sound2);
        }
        sp_sound2.setSelection(db_emergency);

        if (sp_light1 == null) {
            sp_light1 = (Spinner) findViewById(R.id.sp_light1);
        }
        sp_light1.setSelection(db_light1);

        if (sp_light2 == null) {
            sp_light2 = (Spinner) findViewById(R.id.sp_light2);
        }
        sp_light2.setSelection(db_light2);

        if (sp_interval == null) {
            sp_interval = (Spinner) findViewById(R.id.sp_interval);
        }
        sp_interval.setSelection(db_interval);

        /* TOGGLE */
        if (sw_shake == null) {
            sw_shake = (Switch) findViewById(R.id.sw_shake);
        }
        if (db_shake == 1)  sw_shake.setChecked(true);
        else                sw_shake.setChecked(false);

        /* TEXT */
        TextView text_volume1 = (TextView)findViewById(R.id.text_volume1);
        text_volume1.setText(""+db_volume1);

        TextView text_volume2 = (TextView)findViewById(R.id.text_volume2);
        text_volume2.setText(""+db_volume2);

        /* レイアウトのアクティブ表示 */
        LinearLayout lay_normal_11 = (LinearLayout)findViewById(R.id.linearLayout11);
        LinearLayout lay_normal_13 = (LinearLayout)findViewById(R.id.linearLayout13);
        if (soundIsPlayingEmergency()){
            lay_normal_11.setBackgroundResource(R.drawable.btn_grad3);
            lay_normal_13.setBackgroundResource(R.drawable.btn_grad3);
        }
        else{
            lay_normal_11.setBackgroundResource(R.drawable.btn_grad1);
            lay_normal_13.setBackgroundResource(R.drawable.btn_grad1);
        }

        LinearLayout lay_normal_12 = (LinearLayout)findViewById(R.id.linearLayout12);
        if (soundIsPlaying()){
            lay_normal_12.setBackgroundResource(R.drawable.btn_grad3);
        }
        else{
            lay_normal_12.setBackgroundResource(R.drawable.btn_grad1);
        }

        LinearLayout lay_emergency_21 = (LinearLayout)findViewById(R.id.linearLayout21);
        LinearLayout lay_emergency_23 = (LinearLayout)findViewById(R.id.linearLayout23);
        if (soundIsPlayingNormal()){
            lay_emergency_21.setBackgroundResource(R.drawable.btn_grad3);
            lay_emergency_23.setBackgroundResource(R.drawable.btn_grad3);
        }
        else{
            lay_emergency_21.setBackgroundResource(R.drawable.btn_grad2);
            lay_emergency_23.setBackgroundResource(R.drawable.btn_grad2);
        }
        LinearLayout lay_emergency_22 = (LinearLayout)findViewById(R.id.linearLayout22);
        if (soundIsPlaying()){
            lay_emergency_22.setBackgroundResource(R.drawable.btn_grad3);
        }
        else{
            lay_emergency_22.setBackgroundResource(R.drawable.btn_grad2);
        }

    }


    /* **************************************************
        アプリボタン処理
    ****************************************************/
    public void toggleSelect(){
        toggle_normal = (ToggleButton) findViewById(R.id.toggle_normal);
        toggle_emergency = (ToggleButton) findViewById(R.id.toggle_emergency);
        sw_shake = (Switch) findViewById(R.id.sw_shake);

        toggle_normal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    toggle_emergency.setChecked(false);
                    soundStart(1);
                } else {
                    soundStop(1);
                }
                screen_display();
            }
        });

        toggle_emergency.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    toggle_normal.setChecked(false);
                    soundStart(2);
                } else {
                    soundStop(2);
                }
                screen_display();
            }
        });

        sw_shake.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    db_shake = 1;
                } else {
                    db_shake = 0;
                    //  モーション停止
                }
                screen_display();
            }
        });

        screen_display();
    }


    public void seekSelect(){
        //  通常音の音量
        seek_volume1 = (SeekBar)findViewById(R.id.seek_volume1);
        seek_volume1.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    //ツマミをドラッグした時
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (soundIsPlayingEmergency() == false) {
                            db_volume1 = seekBar.getProgress();
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, db_volume1, 0);
                        }
                        screen_display();
                    }
                    //ツマミに触れた時
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }
                    //ツマミを離した時
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );

        //  通常音の音量
        seek_volume2 = (SeekBar)findViewById(R.id.seek_volume2);
        seek_volume2.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    //ツマミをドラッグした時
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        if (soundIsPlayingNormal() == false) {
                            db_volume2 = seekBar.getProgress();
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, db_volume2, 0);
                        }
                        screen_display();
                    }
                    //ツマミに触れた時
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }
                    //ツマミを離した時
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );
    }

    public void spinnerSelect(){

        //  スピナー（通常音）
        sp_sound1 = (Spinner)findViewById(R.id.sp_sound1);
        sp_sound1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //何も選択されなかった時の動作
            @Override
            public void onNothingSelected(AdapterView adapterView) {
            }
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                if (soundIsPlaying() == false){
                    db_normal = position;
                }
                screen_display();
            }
        });
        //  スピナー（SOS音）
        sp_sound2 = (Spinner)findViewById(R.id.sp_sound2);
        sp_sound2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //何も選択されなかった時の動作
            @Override
            public void onNothingSelected(AdapterView adapterView) {
            }
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                if (soundIsPlaying() == false) {
                    db_emergency = position;
                }
                screen_display();
            }
        });

        //  スピナー（通常ライト）
        sp_light1 = (Spinner)findViewById(R.id.sp_light1);
        sp_light1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //何も選択されなかった時の動作
            @Override
            public void onNothingSelected(AdapterView adapterView) {
            }
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                if (soundIsPlaying() == false) {
                    db_light1 = position;
                }
                screen_display();
            }
        });
        //  スピナー（通常ライト）
        sp_light2 = (Spinner)findViewById(R.id.sp_light2);
        sp_light2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //何も選択されなかった時の動作
            @Override
            public void onNothingSelected(AdapterView adapterView) {
            }
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                if (soundIsPlaying() == false) {
                    db_light2 = position;
                }
                screen_display();
            }
        });

        //  スピナー（再生間隔ライト）
        sp_interval = (Spinner)findViewById(R.id.sp_interval);
        sp_interval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //何も選択されなかった時の動作
            @Override
            public void onNothingSelected(AdapterView adapterView) {
            }
            @Override
            public void onItemSelected(AdapterView parent, View view, int position, long id) {
                if (soundIsPlaying() == false) {
                    db_interval = position;
                }
                screen_display();
            }
        });
        screen_display();
    }

    /* **************************************************
        DB初期ロードおよび設定
    ****************************************************/
    public void AppDBInitRoad() {

        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT");
        sql.append(" isopen");
        sql.append(" ,normal");
        sql.append(" ,emergency");
        sql.append(" ,interval");
        sql.append(" ,volume1");
        sql.append(" ,volume2");
        sql.append(" ,light1");
        sql.append(" ,light2");
        sql.append(" ,shake");
        sql.append(" FROM appinfo;");
        try {
            Cursor cursor = db.rawQuery(sql.toString(), null);
            //TextViewに表示
            StringBuilder text = new StringBuilder();
            if (cursor.moveToNext()) {
                db_isopen = cursor.getInt(0);
                db_normal = cursor.getInt(1);
                db_emergency = cursor.getInt(2);
                db_interval = cursor.getInt(3);
                db_volume1 = cursor.getInt(4);
                db_volume2 = cursor.getInt(5);
                db_light1 = cursor.getInt(6);
                db_light2 = cursor.getInt(7);
                db_shake = cursor.getInt(8);
            }
        } finally {
            db.close();
        }

        db = helper.getWritableDatabase();
        if (db_isopen == 0) {
            long ret;
            /* 新規レコード追加 */
            ContentValues insertValues = new ContentValues();
            insertValues.put("isopen", 1);
            insertValues.put("normal", 1);
            insertValues.put("emergency", 4);
            insertValues.put("interval", 1);
            insertValues.put("volume1", 1);
            insertValues.put("volume2", 1);
            insertValues.put("light1", 0);
            insertValues.put("light2", 0);
            insertValues.put("shake", 0);
            insertValues.put("data1", 0);
            insertValues.put("data2", 0);
            insertValues.put("data3", 0);
            insertValues.put("data4", 0);
            insertValues.put("data5", 0);
            insertValues.put("data6", 0);
            insertValues.put("data7", 0);
            insertValues.put("data8", 0);
            insertValues.put("data9", 0);
            insertValues.put("data10", 0);
            try {
                ret = db.insert("appinfo", null, insertValues);
            } finally {
                db.close();
            }
            /*
            if (ret == -1) {
                Toast.makeText(this, "DataBase Create.... ERROR", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "DataBase Create.... OK", Toast.LENGTH_SHORT).show();
            }

             */
        } else {
            /*
            Toast.makeText(this, "Data Loading...  normal:" + db_normal, Toast.LENGTH_SHORT).show();
             */
        }
    }

    /* **************************************************
        DB更新
    ****************************************************/
    public void AppDBUpdated() {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues insertValues = new ContentValues();
        insertValues.put("isopen", db_isopen);
        insertValues.put("normal", db_normal);
        insertValues.put("emergency", db_emergency);
        insertValues.put("interval", db_interval);
        insertValues.put("volume1", db_volume1);
        insertValues.put("volume2", db_volume2);
        insertValues.put("light1", db_light1);
        insertValues.put("light2", db_light2);
        insertValues.put("shake", db_shake);
        int ret;
        try {
            ret = db.update("appinfo", insertValues, null, null);
        } finally {
            db.close();
        }
        /*
        if (ret == -1) {
            Toast.makeText(this, "Saving.... ERROR ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Saving.... OK "+ "op=0:"+db_isopen+" nm=1:"+db_normal+" em=2:"+db_emergency+" it=3:"+db_interval+" v1=4:"+db_volume1+" v2=5:"+db_volume2+" l1=6:"+db_light1+" l2=7:"+db_light2+" sk=8:"+db_shake, Toast.LENGTH_SHORT).show();
        }
        */
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()){
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = event.values.clone(); break;
            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone(); break;
        }
        if(geomagnetic != null && gravity != null) {
            SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
            SensorManager.getOrientation(rotationMatrix, attitude);

            int roll;
            int pitch;
            pitch = (int) (attitude[1] * RAD2DEG);
            roll = (int) (attitude[2] * RAD2DEG);
//            Log.v("回転", "roll[0]=" +(int) (attitude[0] * RAD2DEG) + " roll[1] ="+(int) (attitude[1] * RAD2DEG) + " roll[2] = "+roll + " pit=" + pitch_zero + " rp=" +roll_plus + " rm="+roll_minus);
            if (emergency_playing == false) {
                if (roll> 55 && roll <80) {
                    roll_plus += 1;
                }
                if (roll< -55 && roll >-80) {
                    roll_minus += 1;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /* **************************************************
        スレッド
    ****************************************************/

    /**
     * タイマータスク派生クラス
     * run()に定周期で処理したい内容を記述
     *
     */
    public class MainTimerTask extends TimerTask {
        @Override
        public void run() {
            //ここに定周期で実行したい処理を記述します
            mHandler.post( new Runnable() {
                public void run() {
                    if (countText != null) {
                        //BGMタイマー起動
                        countText.start();
                    }
                }
            });
        }
    }

    /**
     * タイマータスク派生クラス
     * run()に定周期で処理したい内容を記述
     *
     */
    public class EmerTimerTask extends TimerTask {
        @Override
        public void run() {
            //ここに定周期で実行したい処理を記述します
            eHandler.post(new Runnable() {
                public void run() {
                    if (sw_shake == null) {
                        return;
                    }
                    if (!sw_shake.isChecked()){
                        return;
                    }

                    sec_five += 1;
                    if (sec_five <= 3) {    // ３秒以内にイベントを捉えた場合に限り
                        if (roll_plus >= 4 && roll_minus >= 4) {
                            pitch_zero = 0;
                            roll_minus = 0;
                            roll_plus = 0;
                            /* 異常音を再生 */
                            if (toggle_emergency != null){
                                toggle_emergency.setChecked(true);
                                emergency_playing = true;
                            }
                        }
                    }
                    else
                    {
                        pitch_zero = 0;
                        roll_minus = 0;
                        roll_plus = 0;
                        sec_five = 0;
                    }
                }
            });
        }
    }

    /**
     * タイマータスク派生クラス
     * run()に定周期で処理したい内容を記述
     *
     */
    public class BlinkingTask extends TimerTask {
        @Override
        public void run() {
            //ここに定周期で実行したい処理を記述します
            bHandler.post( new Runnable() {
                public void run() {
                    light_on_exec();
                    if (blinking){
                        blinking = false;
                    }
                    else{
                        blinking = true;
                    }
                }
            });
        }
    }


    /* **************************************************
        ライト処理
    ****************************************************/
    /*
     *   ライトＯＮ
     * */
    public void light_on_exec() {
        if(mCameraId == null){
            return;
        }
        try {
            mCameraManager.setTorchMode(mCameraId, blinking);
        } catch (CameraAccessException e) {
            //エラー処理
            e.printStackTrace();
        }
    }
    public void light_ON(int type) {
        if (type == 1){
            blinking = true;
            light_on_exec();
        }
        else if(type == 2){
            this.blinkTimer = new Timer();
            this.blinkTimerTask = new BlinkingTask();
            this.blinkTimer.schedule(blinkTimerTask, 500, 500);
        }
    }
    /*
     *   ライトＯＦＦ
     * */
    public void light_OFF() {

        pitch_zero = 0;
        roll_plus = 0;
        roll_minus = 0;

        if(mCameraId == null){
            return;
        }
        try {
            mCameraManager.setTorchMode(mCameraId, false);
        } catch (CameraAccessException e) {
            //エラー処理
            e.printStackTrace();
        }
    }


    /* **************************************************
        サウンド再生
    ****************************************************/
    public boolean soundIsPlaying(){

        if (this.mainTimer1 != null){
            return true;
        }
        if (this.mainTimer2 != null){
            return true;
        }
        if (this.mainTimer3 != null){
            return true;
        }

        return false;
    }
    public boolean soundIsPlayingNormal(){

        if (this.mainTimer1 != null){
            return true;
        }
        return false;
    }
    public boolean soundIsPlayingEmergency(){

        if (this.mainTimer2 != null){
            return true;
        }
        return false;
    }


    /* 効果音スタート */
    public void soundStart(int type){

        set_interval = soundInterval(db_interval);
        if (set_interval < INTERVAL_1){
            set_interval = 100;
        }

        switch(type) {
            case 1: //通常音
                //タイマーインスタンス生成
                this.mainTimer1 = new Timer();
                //タスククラスインスタンス生成
                this.mainTimerTask1 = new MainTimerTask();
                //タイマースケジュール設定＆開始
                this.mainTimer1.schedule(mainTimerTask1, 500, set_interval);
                //ＢＧＭ
                soundSelect(db_normal);

                if (this.mainTimer2 != null) {
                    this.mainTimer2.cancel();
                    this.mainTimer2 = null;
                }
                if (this.mainTimer3 != null) {
                    this.mainTimer3.cancel();
                    this.mainTimer3 = null;
                }
                //音量調整
                am.setStreamVolume(AudioManager.STREAM_MUSIC, db_volume1, 0);

                //ライトON
                light_ON(db_light1);
                break;

            case 2: //異常音
                //タイマーインスタンス生成
                this.mainTimer2 = new Timer();
                //タスククラスインスタンス生成
                this.mainTimerTask2 = new MainTimerTask();
                //タイマースケジュール設定＆開始
                this.mainTimer2.schedule(mainTimerTask2, 500, set_interval);
                //ＢＧＭ
                soundSelect(db_emergency);

                if (this.mainTimer1 != null) {
                    this.mainTimer1.cancel();
                    this.mainTimer1 = null;
                }
                if (this.mainTimer3 != null) {
                    this.mainTimer3.cancel();
                    this.mainTimer3 = null;
                }
                //音量調整
                am.setStreamVolume(AudioManager.STREAM_MUSIC, db_volume2, 0);

                //ライトON
                light_ON(db_light2);
                break;

            case 3: //緊急音
                //タイマーインスタンス生成
                this.mainTimer3 = new Timer();
                //タスククラスインスタンス生成
                this.mainTimerTask3 = new MainTimerTask();
                //タイマースケジュール設定＆開始
                this.mainTimer3.schedule(mainTimerTask3, 500, set_interval);
                //ＢＧＭ
                soundSelect(db_emergency);

                if (this.mainTimer1 != null) {
                    this.mainTimer1.cancel();
                    this.mainTimer1 = null;
                }
                if (this.mainTimer2 != null) {
                    this.mainTimer2.cancel();
                    this.mainTimer2 = null;
                }
                //音量調整
                am.setStreamVolume(AudioManager.STREAM_MUSIC, db_volume2, 0);
                break;
        }
    }

    /* 効果音ストップ */
    public void soundStop(int type){
        emergency_playing = false;

        light_OFF();

        if (this.mainTimer1 != null) {
            this.mainTimer1.cancel();
            this.mainTimer1 = null;
        }
        if (this.mainTimer2 != null) {
            this.mainTimer2.cancel();
            this.mainTimer2 = null;
        }
        if (this.mainTimer3 != null) {
            this.mainTimer3.cancel();
            this.mainTimer3 = null;
        }
        if (this.blinkTimer != null) {
            this.blinkTimer.cancel();
            this.blinkTimer = null;
        }
    }

    public void soundSelect(int type){

        switch (type){
            default:
                this.countText = null;
            case 0:
                this.countText = null;
                break;
            case 1:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.bell_1);
                break;
            case 2:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.bell_2);
                break;
            case 3:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.bell_3);
                break;
            case 4:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.thunder_1);
                break;
            case 5:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.thunder_2);
                break;
            case 6:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.thunder_3);
                break;
            case 7:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.firecracker);
                break;
            case 8:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.firework);
                break;
            case 9:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.radio_1);
                break;
            case 10:
                this.countText = (MediaPlayer) MediaPlayer.create(this, R.raw.whistle);
                break;
        }
    }

    public int soundInterval(int type) {

        switch (type) {
            case 0:
                return INTERVAL_0;
            case 1:
                return INTERVAL_1;
            case 2:
                return INTERVAL_3;
            case 3:
                return INTERVAL_5;
            case 4:
                return INTERVAL_7;
            case 5:
                return INTERVAL_10;
            case 6:
                return INTERVAL_15;
            case 7:
                return INTERVAL_20;
            case 8:
                return INTERVAL_30;
        }
        return INTERVAL_0;
    }

}