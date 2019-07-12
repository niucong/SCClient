package com.niucong.scclient;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.niucong.scclient.db.DrugInfoDB;
import com.niucong.scclient.db.SellRecord;
import com.niucong.scclient.net.ApiCallback;
import com.niucong.scclient.net.ReturnData;
import com.niucong.scclient.util.BluetoothChatService;
import com.niucong.scclient.util.FileUtil;
import com.niucong.scclient.util.NiftyDialogBuilder;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.litepal.LitePal;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.RequestBody;

public class MainActivity extends BasicActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private String TAG = "MainActivity";

    private RecyclerView mRecyclerView;
    private TextView tv_total, nav_title, nav_total, nav_warn;// , nav_time

    private List<SellRecord> uRecords;
    private HomeAdapter mAdapter;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothChatService.stop();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
//        MobclickAgent.onEvent(this, "0");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        nav_title = (TextView) headerView.findViewById(R.id.nav_title);
        nav_total = (TextView) headerView.findViewById(R.id.nav_total);
        nav_warn = (TextView) headerView.findViewById(R.id.nav_warn);

        mRecyclerView = (RecyclerView) findViewById(R.id.main_rv);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter = new HomeAdapter());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));

        App.app.list = LitePal.findAll(DrugInfoDB.class);
        setSearchBar(this, true);
        uRecords = new ArrayList<>();

        tv_total = (TextView) findViewById(R.id.main_total);
        findViewById(R.id.main_btn).setOnClickListener(this);
        mRecyclerView.requestFocus();

//        connection();
//        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS));

        et_search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (KeyEvent.ACTION_DOWN == event.getAction()) {
//                    Log.d(TAG, "onCreate keyCode=" + keyCode);
                    if (KeyEvent.KEYCODE_ENTER == keyCode) {
                        if (TextUtils.isEmpty(et_search.getText().toString())) {
                            //处理事件
                            sendOrder();
                            return true;
                        }
                    } else if (keyCode == 111) {
                        mAdapter.notifyDataSetChanged();
                        tv_total.setText("合计：0.0");
                        return true;
                    }
                }
                return false;
            }
        });
        // 蓝牙连接
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //获取本地蓝牙实例
        if (mBluetoothAdapter.isEnabled() && !TextUtils.isEmpty(App.app.share.getStringMessage("SC", "MAC", ""))) {
            bluetoothChatService = BluetoothChatService.getInstance(handler);
            bluetoothChatService.start();
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(App.app.share.getStringMessage("SC", "MAC", ""));
            bluetoothChatService.connectDevice(device);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setNavTip();
        if (App.app.refresh) {
            App.app.refresh = false;
            setSearchBar(this, true);
        }
    }

    private void setNavTip() {
//        nav_total.setText("今日销售额：" + app.showPrice(total));
//        nav_warn.setText(warn + " 种需要进货");
        nav_title.setText(getText(R.string.app_name));
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.main_btn:
                // 结算
                sendOrder();
                break;
        }
    }

    private void sendOrder() {
        // TODO
//       PrintUtil.printStick(mGpService, sRecords);
        uRecords.clear();
        mAdapter.notifyDataSetChanged();
        tv_total.setText("合计：0.0");
    }

    @Override
    protected boolean searchDrug(String result) {
        Log.d(TAG, "searchDrug code=" + result);
        if (TextUtils.isEmpty(result)) {
//            Snackbar.make(tv_total, "条形码输入错误", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
            return false;
        }
        DrugInfoDB drugInfoDB = LitePal.where("barCode = ?", result).findFirst(DrugInfoDB.class);
        if (drugInfoDB != null) {
            SellRecord sr = null;
            long c = drugInfoDB.getBarCode();
            if (uRecords.size() > 0) {
                for (SellRecord uRecord : uRecords) {
                    if (c == uRecord.getBarCode()) {
                        sr = uRecord;
                        break;
                    }
                }
            }
            if (sr == null) {
                sr = new SellRecord();
                sr.setBarCode(drugInfoDB.getBarCode());
                sr.setName(drugInfoDB.getName());
                sr.setFactory(drugInfoDB.getFactory());
                sr.setPrice(drugInfoDB.getPrice());
                sr.setNumber(1);
            } else {
                sr.setNumber(sr.getNumber() + 1);
                uRecords.remove(sr);
            }
            uRecords.add(0, sr);
            mAdapter.notifyDataSetChanged();
            getTotalPrice();
//            et_search.setText("");
//            mRecyclerView.requestFocus();
            return true;
        } else {
            App.app.showToast("该药品不在库存中,请先添加入库");
        }
        return false;
    }

    private void getTotalPrice() {
        int total = 0;
        for (SellRecord di : uRecords) {
            total += di.getPrice() * di.getNumber();
        }
        tv_total.setText("合计：" + App.app.showPrice(total));
    }

    class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.MyViewHolder> {
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            MyViewHolder holder = new MyViewHolder(LayoutInflater.from(MainActivity.this).inflate(R.layout.item_home, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, final int position) {
            final SellRecord sr = uRecords.get(position);
            final long code = sr.getBarCode();
            holder.tv_code.setText("" + code);
            holder.tv_price.setText(App.app.showPrice(sr.getPrice()));
            holder.tv_num.setText("" + sr.getNumber());
            holder.tv_subPrice.setText("小计：" + App.app.showPrice(sr.getPrice() * sr.getNumber()));

            holder.tv_name.setText(sr.getName());
            holder.tv_factory.setText(sr.getFactory());

            holder.iv_delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uRecords.remove(sr);
                    notifyDataSetChanged();
                    getTotalPrice();
                }
            });

            holder.tv_remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int num = sr.getNumber();
                    if (num > 1) {
                        sr.setNumber(num - 1);
                        notifyDataSetChanged();
                        getTotalPrice();
                    }
                }
            });

            holder.tv_add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int num = sr.getNumber();
                    sr.setNumber(num + 1);
                    notifyDataSetChanged();
                    getTotalPrice();
                }
            });
        }

        @Override
        public int getItemCount() {
            return uRecords.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView tv_name, tv_code, tv_factory, tv_price, tv_num, tv_remove, tv_add, tv_subPrice;
            ImageView iv_delete;

            public MyViewHolder(View view) {
                super(view);
                tv_name = (TextView) view.findViewById(R.id.item_home_name);
                tv_code = (TextView) view.findViewById(R.id.item_home_code);
                tv_factory = (TextView) view.findViewById(R.id.item_home_factory);
                tv_price = (TextView) view.findViewById(R.id.item_home_pirce);
                tv_num = (TextView) view.findViewById(R.id.item_home_num);
                tv_remove = (TextView) view.findViewById(R.id.item_home_remove);
                tv_add = (TextView) view.findViewById(R.id.item_home_add);
                tv_subPrice = (TextView) view.findViewById(R.id.item_home_subPrice);

                iv_delete = (ImageView) view.findViewById(R.id.item_home_delete);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_drug) {// 查看药品
            startActivity(new Intent(this, DrugListActivity.class));
        } else if (id == R.id.nav_connet) {// 设置连接方式：wifi/蓝牙
            settingDialog(0);
        } else if (id == R.id.nav_sysn) {// 同步数据
            synData(0);
        } else if (id == R.id.nav_data) {// 导出数据
            //6.0运行权限设置
            if (!FileUtil.setPermission(MainActivity.this, MainActivity.this, Manifest
                    .permission.READ_EXTERNAL_STORAGE, 1) || !FileUtil.setPermission(MainActivity.this, MainActivity.this, Manifest
                    .permission.WRITE_EXTERNAL_STORAGE, 1)) {
                SimpleDateFormat YMDHM = new SimpleDateFormat("yyyyMMddHHmm");
                final String path = FileUtil.getSdcardDir() + "/药品表_" + YMDHM.format(new Date()) + ".xls";
                App.app.showToast("正在导出药品表……");
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            saveExcel(path);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    App.app.showToast("已导出到" + path);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    App.app.showToast("导出失败");
                                }
                            });
                        }
                        super.run();
                    }
                }.start();
            }
        } else if (id == R.id.nav_printer) {// 连接打印机
//            if (mGpService == null) {
//                Toast.makeText(this, "Print Service is not start, please check it", Toast.LENGTH_SHORT).show();
//            } else {
//                Intent intent = new Intent(this, PrinterConnectDialog.class);
//                boolean[] state = getConnectState();
//                intent.putExtra("connect.status", state);
//                this.startActivity(intent);
//            }
        } else if (id == R.id.nav_init) {// 初始备份数据
            settingDialog(1);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == 1) {
            // TODO
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 导出数据
     */
    private void saveExcel(String path) throws Exception {
//1、输出的文件地址及名称
        OutputStream out = new FileOutputStream(path);
//2、sheet表中的标题行内容，需要输入excel的汇总数据
        String[] summary = {"条形码", "名称", "厂商", "价格"};
        List<List<String>> summaryData = new ArrayList<List<String>>();
        List<DrugInfoDB> dbs = LitePal.findAll(DrugInfoDB.class);
        for (DrugInfoDB db : dbs) {
            List<String> rowData = new ArrayList<String>();
            rowData.add(db.getBarCode() + "");
            rowData.add(db.getName());
            rowData.add(db.getFactory());
            rowData.add(App.app.showPrice(db.getPrice()));
            summaryData.add(rowData);
        }
        HSSFWorkbook workbook = new HSSFWorkbook();
//第一个表格内容
        FileUtil.exportExcel(workbook, 0, "药品表", summary, summaryData);
//第二个表格内容
//            exportExcel(workbook, 1, "部分流水数据", water, waterData);
//将所有的数据一起写入，然后再关闭输入流。
        workbook.write(out);
        out.close();
    }

    /**
     * 封装同步数据、初始备份数据
     *
     * @param type：0同步数据、1初始化数据、2上传所有数据
     * @return
     */
    private JSONObject postData(int type) {
        JSONObject jsonObject = new JSONObject();
        long updateTime = App.app.share.getLongMessage("SC", "updateTime", 0);
        if (type != 0) {
            updateTime = 0;
        }
        jsonObject.put("updateTime", updateTime);
        List<DrugInfoDB> list = LitePal.where("updateTime > ?", "" + updateTime).find(DrugInfoDB.class);
        JSONArray array = new JSONArray();
        for (DrugInfoDB drugInfo : list) {
            JSONObject json = new JSONObject();
            json.put("barCode", drugInfo.getBarCode());
            json.put("name", drugInfo.getName());
            json.put("factory", drugInfo.getFactory());
            json.put("namePY", drugInfo.getNamePY());
            json.put("namePYF", drugInfo.getNamePYF());
            json.put("updateTime", drugInfo.getUpdateTime());
            json.put("price", drugInfo.getPrice());
            array.add(json);
        }
        jsonObject.put("data", array);
        return jsonObject;
    }

    /**
     * 同步数据、初始备份数据
     *
     * @param type：0同步数据、1初始化数据、2上传所有数据
     */
    private void synData(int type) {
        if (App.app.share.getIntMessage("SC", "ConnetType", 0) == 1) {// Wifi连接
            if (TextUtils.isEmpty(App.app.share.getStringMessage("SC", "IP", ""))) {
                App.app.showToast("请先设置连接地址");
            } else {
                App.app.showToast("正在同步数据");
                RequestBody bodyList = RequestBody.create(okhttp3.MediaType.parse("application/json;charset=UTF-8"), postData(type).toString());
                addSubscription(getApi().synData(bodyList), new ApiCallback<ReturnData>() {
                    @Override
                    public void onSuccess(ReturnData returnData) {
                        dealData(returnData);
                    }

                    @Override
                    public void onFinish() {

                    }

                    @Override
                    public void onFailure(String msg) {
                        App.app.showToast("数据同步失败，请检查连接");
                    }
                });
            }
        } else {// 蓝牙连接
            if (TextUtils.isEmpty(App.app.share.getStringMessage("SC", "MAC", ""))) {
                App.app.showToast("请先设置连接地址");
            } else {
                App.app.showToast("正在同步数据");
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //获取本地蓝牙实例
                if (mBluetoothAdapter.isEnabled()) {// 蓝牙是否打开
                    // 开启蓝牙服务
                    JSONObject jsonObject = postData(type);
                    jsonObject.put("method", "synData");
                    result = "";
                    bluetoothChatService.sendData(jsonObject.toString().getBytes());
                } else {
                    App.app.showToast("请先打开蓝牙");
                }
            }
        }
    }

    /**
     * 处理同步数据
     *
     * @param returnData
     */
    public void dealData(ReturnData returnData) {
        if (returnData == null) {
//            App.app.showToast("数据同步失败，返回值为空");
            return;
        }
        if (returnData.getCode() == 200) {
            try {
                List<DrugInfoDB> list = JSON.parseArray(returnData.getData().toString(), DrugInfoDB.class);
                Log.d("MainActivity", "size=" + list.size());
                for (DrugInfoDB db : list) {
                    DrugInfoDB locDb = LitePal.where("barCode = ?", "" + db.getBarCode()).findFirst(DrugInfoDB.class);
                    if (locDb == null || db.getUpdateTime() > locDb.getUpdateTime()) {
                        db.save();
                    }
                }
                App.app.share.saveLongMessage("SC", "updateTime", System.currentTimeMillis());
                App.app.showToast("数据同步成功");
            } catch (Exception e) {
                e.printStackTrace();
                App.app.showToast("数据同步失败，返回值类型错误");
            }
        } else {
            App.app.showToast("数据同步失败" + returnData.getCode() + "，" + returnData.getMsg());
        }
    }

    /**
     * 设置连接方式、初始备份数据
     *
     * @param type：0设置连接方式、1初始备份数据
     */
    private void settingDialog(final int type) {
        final NiftyDialogBuilder submitDia = NiftyDialogBuilder.getInstance(this);
        View settingView = LayoutInflater.from(this).inflate(R.layout.dialog_setting, null);
        final RadioButton rb1 = (RadioButton) settingView.findViewById(R.id.radioButton1);
        RadioButton rb2 = (RadioButton) settingView.findViewById(R.id.radioButton2);
        final EditText et_ip = (EditText) settingView.findViewById(R.id.et_ip);

        //Environment.getExternalStorageDirectory();// /storage/usbotg、/storage/081C-9F49
//        final String SDCardPath = App.app.share.getStringMessage("SC", "USBPath", "/storage/usbotg") + File.separator;

        if (type == 0) {
            submitDia.withTitle("选择连接方式");
            if (App.app.share.getIntMessage("SC", "ConnetType", 0) == 0) {
                rb1.setChecked(true);
                et_ip.setHint("蓝牙MAC地址");
                et_ip.setText(App.app.share.getStringMessage("SC", "MAC", ""));
            } else {
                rb2.setChecked(true);
                et_ip.setHint("IP地址");
                et_ip.setText(App.app.share.getStringMessage("SC", "IP", ""));
            }
            ((RadioGroup) settingView.findViewById(R.id.radioGroup)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.radioButton1:
                            et_ip.setText(App.app.share.getStringMessage("SC", "MAC", ""));
                            et_ip.setHint("蓝牙MAC地址");
                            break;
                        case R.id.radioButton2:
                            et_ip.setText(App.app.share.getStringMessage("SC", "IP", ""));
                            et_ip.setHint("IP地址");
                            break;
                    }
                }
            });
        } else {
            submitDia.withTitle("初始备份数据");
            rb1.setText("初始化数据");
            rb2.setText("上传所有数据");
            et_ip.setHint("请输入管理员密码");
        }


        submitDia.withButton2Text("确定", 0).setButton2Click(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = et_ip.getText().toString();
                if (type == 0) {
                    if (TextUtils.isEmpty(ip)) {
                        App.app.showToast("请输入正确的地址");
                        return;
                    }
                    if (rb1.isChecked()) {
                        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //获取本地蓝牙实例
                        if (mBluetoothAdapter.isEnabled()) {// 蓝牙是否打开
                            App.app.share.saveStringMessage("SC", "MAC", ip);
                            App.app.share.saveIntMessage("SC", "ConnetType", 0);
                            App.app.showToast("已选择蓝牙连接");

                            // 开启蓝牙服务
                            bluetoothChatService = BluetoothChatService.getInstance(handler);
                            bluetoothChatService.start();
                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(ip);
                            bluetoothChatService.connectDevice(device);
                        } else {
                            App.app.showToast("请先打开蓝牙");
                        }
                    } else {
                        App.app.share.saveStringMessage("SC", "IP", ip);
                        App.app.share.saveIntMessage("SC", "ConnetType", 1);
                        App.app.showToast("已选择Wifi连接");
                    }
                    submitDia.dismiss();
                } else {
                    if (!"admin".equals(ip)) {
                        App.app.showToast("请输入正确的密码");
                    } else {
                        if (rb1.isChecked()) {
                            synData(1);
                        } else {
                            synData(2);
                        }
                    }
                    submitDia.dismiss();
                }
            }
        });
        submitDia.setCustomView(settingView, this);
        submitDia.withMessage(null).withDuration(400);
        submitDia.isCancelable(false);
        submitDia.show();
    }

    public static String result = "";

    private BluetoothChatService bluetoothChatService;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BluetoothChatService.BLUE_TOOTH_READ: {
//                    byte[] readBuf = (byte[]) msg.obj;
//                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    Log.d("MainActivity", readMessage);
//                    result += readMessage;

                    if (result.endsWith("}") && result.contains("code")) {
                        try {
                            dealData(JSON.parseObject(result, ReturnData.class));
                            result = "";
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            }
        }
    };

}
