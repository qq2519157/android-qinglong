package auto.qinglong.database.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import auto.qinglong.MyApplication;
import auto.qinglong.bean.app.Statistics;
import auto.qinglong.utils.LogUnit;

public class StatisticsDBHelper {
    public static final String TAG = "StatisticsDBHelper";
    private static final DBHelper DBHelper;
    public static final String key_module = "module";
    public static final String key_start_time = "startTime";
    public static final String key_end_time = "endTime";
    public static final String key_num = "num";
    private static boolean canReport = false;
    private static String reportUrl = null;

    static {
        DBHelper = new DBHelper(MyApplication.getContext(), auto.qinglong.database.db.DBHelper.DB_NAME, null, auto.qinglong.database.db.DBHelper.VERSION);
    }

    @SuppressLint("Range")
    public static List<Statistics> getAll() {
        List<Statistics> list = new ArrayList<>();
        try {
            Cursor cursor = DBHelper.getWritableDatabase().query(auto.qinglong.database.db.DBHelper.TABLE_STATISTICS, null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    Statistics statistics = new Statistics();
                    statistics.setModule(cursor.getString(cursor.getColumnIndex(key_module)));
                    statistics.setStartTime(cursor.getInt(cursor.getColumnIndex(key_start_time)));
                    statistics.setEndTime(cursor.getInt(cursor.getColumnIndex(key_end_time)));
                    statistics.setNum(cursor.getInt(cursor.getColumnIndex(key_num)));
                    list.add(statistics);
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            LogUnit.log(TAG, e.getMessage());
        }
        return list;
    }

    /**
     * 统计
     *
     * @param module 模块
     */
    public static void increase(@NonNull String module) {
        LogUnit.log(module);
        if (isExist(module)) {
            update(module);
        } else {
            insert(module);
        }
    }

    public static void clear() {
        try {
            DBHelper.getWritableDatabase().delete(auto.qinglong.database.db.DBHelper.TABLE_STATISTICS, null, null);
        } catch (Exception e) {
            LogUnit.log(TAG, e.getMessage());
        }
    }

    private static void insert(@NonNull String module) {
        try {
            ContentValues values = new ContentValues();
            values.put(key_module, module);
            values.put(key_start_time, System.currentTimeMillis() / 1000);
            values.put(key_end_time, System.currentTimeMillis() / 1000);
            values.put(key_num, 1);

            DBHelper.getWritableDatabase().insert(auto.qinglong.database.db.DBHelper.TABLE_STATISTICS, null, values);
        } catch (Exception e) {
            LogUnit.log(TAG, e.getMessage());
        }
    }

    private static void update(@NonNull String module) {
        try {
            String sql = String.format(Locale.ENGLISH, "UPDATE  module = %s SET num = num + 1, endTime = %d WHERE module = %s", auto.qinglong.database.db.DBHelper.TABLE_STATISTICS, System.currentTimeMillis() / 1000, module);
            DBHelper.getWritableDatabase().execSQL(sql);
        } catch (Exception e) {
            LogUnit.log(TAG, e.getMessage());
        }
    }

    private static boolean isExist(String module) {
        try {
            String where = "module = ?";
            Cursor cursor = DBHelper.getWritableDatabase().query(auto.qinglong.database.db.DBHelper.TABLE_STATISTICS, null, where, new String[]{String.valueOf(module)}, null, null, null);
            boolean flag = cursor.moveToFirst();
            cursor.close();
            return flag;
        } catch (Exception e) {
            LogUnit.log(TAG, e.getMessage());
            return true;
        }
    }

    public static boolean isCanReport() {
        return canReport;
    }

    public static void setCanReport(boolean canReport) {
        StatisticsDBHelper.canReport = canReport;
    }

    public static String getReportUrl() {
        return reportUrl;
    }

    public static void setReportUrl(String reportUrl) {
        StatisticsDBHelper.reportUrl = reportUrl;
    }
}

