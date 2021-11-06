package tfsapps.beastbusterpro;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }
    @Override
    public void onResume() {
        super.onResume();
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
}