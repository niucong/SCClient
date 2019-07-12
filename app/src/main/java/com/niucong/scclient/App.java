package com.niucong.scclient;

import android.app.Application;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.niucong.scclient.db.DrugInfoDB;
import com.niucong.scclient.util.AppSharedPreferences;

import org.litepal.LitePal;

import java.util.Formatter;
import java.util.List;

public class App extends Application {

    public static App app;
    public AppSharedPreferences share;
    public List<DrugInfoDB> list;
    public boolean refresh;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        share = new AppSharedPreferences(this);

        LitePal.initialize(this);
        Stetho.initializeWithDefaults(this);
    }

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * 保存价格-String转int
     *
     * @param price
     * @return
     */
    public int savePrice(String price) {
        try {
            return (int) (Float.valueOf(price) * 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 显示价格-int转String
     *
     * @param price
     * @return
     */
    public String showPrice(long price) {
        float pf = price;
        return new Formatter().format("%.2f", pf / 100).toString();
    }
}
