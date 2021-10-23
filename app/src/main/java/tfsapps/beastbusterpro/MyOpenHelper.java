package tfsapps.beastbusterpro;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyOpenHelper extends SQLiteOpenHelper
{
    private static final String TABLE = "appinfo";
    public MyOpenHelper(Context context) {
        super(context, "AppDB", null, 1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + "("
                + "isopen integer,"             //DBオープン
                + "normal integer,"             //通常音   タイプ
                + "emergency integer,"          //緊急音   タイプ
                + "interval integer,"           //間隔     連続再生時
                + "volume1 integer,"            //音量１    （通常音）
                + "volume2 integer,"            //音量２    （緊急音）
                + "light1 integer,"             //ライト１  （消灯、点灯、点滅）
                + "light2 integer,"             //ライト２  （消灯、点灯、点滅）
                + "shake integer,"              //振る機能  （する／しない）
                + "data1 integer,"              //予備１～１０
                + "data2 integer,"
                + "data3 integer,"
                + "data4 integer,"
                + "data5 integer,"
                + "data6 integer,"
                + "data7 integer,"
                + "data8 integer,"
                + "data9 integer,"
                + "data10 integer);");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}