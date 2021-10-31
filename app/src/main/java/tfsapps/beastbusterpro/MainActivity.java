package tfsapps.beastbusterpro;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private MyOpenHelper helper;    //DBアクセス
    private int db_isopen = 0;      //DB使用したか
    private int db_normal = 0;      //DB通常音タイプ
    private int db_emergency = 0;   //DB緊急音タイプ
    private int db_interval = 0;    //DB連続再生間隔
    private int db_volume1 = 0;     //DB音量（通常音）
    private int db_volume2 = 0;     //DB音量（緊急音）
    private int db_light1 = 0;       //DBライト（通常音）
    private int db_light2 = 0;       //DBライト（緊急音）
    private int db_shake = 0;       //DB振る

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }



    @Override
    public void onStart() {
        super.onStart();
        //DBのロード
        /* データベース */
        helper = new MyOpenHelper(this);
        AppDBInitRoad();
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onPause(){
        super.onPause();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
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
            Toast.makeText(this, "Saving.... OK ", Toast.LENGTH_SHORT).show();
        }
    }
}