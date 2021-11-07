package tfsapps.beastbusterpro;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
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
    private MediaPlayer bgm;
    private MediaPlayer countText;			//テキストビュー
    //  スレッド
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  国設定
        _local = Locale.getDefault();
        _language = _local.getLanguage();
        _country = _local.getCountry();

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
    }

    //  戻るボタン
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            // 戻るボタンの処理
            // ダイアログ表示など特定の処理を行いたい場合はここに記述
            // 親クラスのdispatchKeyEvent()を呼び出さずにtrueを返す
            if (this.mainTimer1 == null && this.mainTimer2 == null && this.mainTimer3 == null) {
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
    }


    /* **************************************************
        アプリボタン処理
    ****************************************************/
    public void toggleSelect(){
        toggle_normal = (ToggleButton) findViewById(R.id.toggle_normal);
        toggle_normal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //  再生
                    //  異常停止
                } else {
                    //  停止
                }
            }
        });

        toggle_emergency = (ToggleButton) findViewById(R.id.toggle_emergency);
        toggle_emergency.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //  再生
                    //  通常停止
                } else {
                    //  停止
                }
            }
        });

        sw_shake = (Switch) findViewById(R.id.sw_shake);
        sw_shake.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    db_shake = 1;
                } else {
                    db_shake = 0;
                    //  モーション停止
                }
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
                        db_volume1 = seekBar.getProgress();
//                      am.setStreamVolume(AudioManager.STREAM_MUSIC, now_volume, 0);
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
                        db_volume2 = seekBar.getProgress();
//                      am.setStreamVolume(AudioManager.STREAM_MUSIC, now_volume, 0);
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
                db_normal = position;
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
                db_emergency = position;
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
                db_light1 = position;
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
                db_light2 = position;
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
                db_interval = position;
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
            insertValues.put("normal", 0);
            insertValues.put("emergency", 0);
            insertValues.put("interval", 0);
            insertValues.put("volume1", 0);
            insertValues.put("volume2", 0);
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
            if (ret == -1) {
                Toast.makeText(this, "DataBase Create.... ERROR", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "DataBase Create.... OK", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Data Loading...  normal:" + db_normal, Toast.LENGTH_SHORT).show();
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
        if (ret == -1) {
            Toast.makeText(this, "Saving.... ERROR ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Saving.... OK "+ "op=0:"+db_isopen+" nm=1:"+db_normal+" em=2:"+db_emergency+" it=3:"+db_interval+" v1=4:"+db_volume1+" v2=5:"+db_volume2+" l1=6:"+db_light1+" l2=7:"+db_light2+" sk=8:"+db_shake, Toast.LENGTH_SHORT).show();
        }
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
                    //BGMタイマー起動
                    countText.start();
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
                    sec_five += 1;
                    if (sec_five <= 3) {    // ３秒以内にイベントを捉えた場合に限り
                        if (roll_plus >= 4 && roll_minus >= 4) {
                            pitch_zero = 0;
                            roll_minus = 0;
                            roll_plus = 0;
                            /* 異常音を再生 */
//                            emergency_Start();
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

}