package com.niucong.scclient;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;
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
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.android.material.navigation.NavigationView;
import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.GpDevice;
import com.gprinter.service.GpPrintService;
import com.niucong.printer.PrinterConnectDialog;
import com.niucong.scclient.db.DrugInfoDB;
import com.niucong.scclient.db.SellRecord;
import com.niucong.scclient.net.ApiCallback;
import com.niucong.scclient.net.ReturnData;
import com.niucong.scclient.util.BluetoothChatService;
import com.niucong.scclient.util.FileUtil;
import com.niucong.scclient.util.NiftyDialogBuilder;
import com.niucong.scclient.util.PrintUtil;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.litepal.LitePal;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

        connection();
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS));

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
        if (mBluetoothAdapter.isEnabled() && !TextUtils.isEmpty(App.app.share.getString("MAC", ""))) {
            bluetoothChatService = BluetoothChatService.getInstance(handler);
            bluetoothChatService.start();
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(App.app.share.getString("MAC", ""));
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
        long updateTime = App.app.share.getLong("updateTime", 0);
        if (updateTime > 0) {
            nav_total.setText("上次同步时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(updateTime)));
        }

        if (App.app.share.getInt("ConnetType", 0) == 1) {
            nav_title.setText("Wifi连接 " + App.app.share.getString("IP", ""));
        } else {
            nav_title.setText("蓝牙连接 " + App.app.share.getString("MAC", ""));
        }
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
        PrintUtil.printStick(mGpService, uRecords);
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
            if (!FileUtil.setPermission(MainActivity.this, MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, 2)) {
                exportExcel();
            }
        } else if (id == R.id.nav_printer) {// 连接打印机
            if (mGpService == null) {
                Toast.makeText(this, "Print Service is not start, please check it", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, PrinterConnectDialog.class);
                boolean[] state = getConnectState();
                intent.putExtra("connect.status", state);
                this.startActivity(intent);
            }
        } else if (id == R.id.nav_init) {// 初始备份数据
            settingDialog(1);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void exportExcel() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == 2) {
            exportExcel();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private GpService mGpService = null;
    private PrinterServiceConnection conn = null;

    private int mPrinterIndex = 0;
    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private static final int REQUEST_PRINT_LABEL = 0xfd;
    private static final int REQUEST_PRINT_RECEIPT = 0xfc;

    class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("ServiceConnection", "onServiceDisconnected() called");
            mGpService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGpService = GpService.Stub.asInterface(service);
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // GpCom.ACTION_DEVICE_REAL_STATUS 为广播的IntentFilter
            if (action.equals(GpCom.ACTION_DEVICE_REAL_STATUS)) {

                // 业务逻辑的请求码，对应哪里查询做什么操作
                int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);
                // 判断请求码，是则进行业务操作
                if (requestCode == MAIN_QUERY_PRINTER_STATUS) {

                    int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    String str;
                    if (status == GpCom.STATE_NO_ERR) {
                        str = "打印机正常";
                    } else {
                        str = "打印机 ";
                        if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {
                            str += "脱机";
                        }
                        if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0) {
                            str += "缺纸";
                        }
                        if ((byte) (status & GpCom.STATE_COVER_OPEN) > 0) {
                            str += "打印机开盖";
                        }
                        if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {
                            str += "打印机出错";
                        }
                        if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {
                            str += "查询超时";
                        }
                    }

                    Toast.makeText(getApplicationContext(), "打印机：" + mPrinterIndex + " 状态：" + str, Toast.LENGTH_SHORT)
                            .show();
                } else if (requestCode == REQUEST_PRINT_LABEL) {
                    int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    if (status == GpCom.STATE_NO_ERR) {
                        sendLabel();
                    } else {
                        Toast.makeText(MainActivity.this, "query printer status error", Toast.LENGTH_SHORT).show();
                    }
                } else if (requestCode == REQUEST_PRINT_RECEIPT) {
                    int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    if (status == GpCom.STATE_NO_ERR) {
                        sendReceipt();
                    } else {
                        Toast.makeText(MainActivity.this, "query printer status error", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    void sendLabel() {
        LabelCommand tsc = new LabelCommand();
        tsc.addSize(60, 60); // 设置标签尺寸，按照实际尺寸设置
        tsc.addGap(0); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向
        tsc.addReference(0, 0);// 设置原点坐标
        tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
        tsc.addCls();// 清除打印缓冲区
        // 绘制简体中文
        tsc.addText(20, 20, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "Welcome to use SMARNET printer!");
        // 绘制图片
        Bitmap b = BitmapFactory.decodeResource(getResources(), com.niucong.printer.R.drawable.gprinter);
        tsc.addBitmap(20, 50, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);

        tsc.addQRCode(250, 80, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.smarnet.cc");
        // 绘制一维条码
        tsc.add1DBarcode(20, 250, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "SMARNET");
        tsc.addPrint(1, 1); // 打印标签
        tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rel;
        try {
            rel = mGpService.sendLabelCommand(mPrinterIndex, str);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void sendReceipt() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
        esc.addText("Sample\n"); // 打印文字
        esc.addPrintAndLineFeed();

        /* 打印文字 */
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐
        esc.addText("Print text\n"); // 打印文字
        esc.addText("Welcome to use SMARNET printer!\n"); // 打印文字

        /* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        // esc.addText(message,"BIG5");
        esc.addText(message, "GB2312");
        esc.addPrintAndLineFeed();

        /* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText("智汇");
        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("网络");
        esc.addSetAbsolutePrintPosition((short) 10);
        esc.addText("设备");
        esc.addPrintAndLineFeed();

        /* 打印图片 */
        // esc.addText("Print bitmap!\n"); // 打印文字
        // Bitmap b = BitmapFactory.decodeResource(getResources(),
        // R.drawable.gprinter);
        // esc.addRastBitImage(b, b.getWidth(), 0); // 打印图片

        /* 打印一维条码 */
        esc.addText("Print code128\n"); // 打印文字
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);//
        // 设置条码可识别字符位置在条码下方
        esc.addSetBarcodeHeight((byte) 60); // 设置条码高度为60点
        esc.addSetBarcodeWidth((byte) 1); // 设置条码单元宽度为1
        esc.addCODE128(esc.genCodeB("SMARNET")); // 打印Code128码
        esc.addPrintAndLineFeed();

        /*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         */
        esc.addText("Print QRcode\n"); // 打印文字
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31); // 设置纠错等级
        esc.addSelectSizeOfModuleForQRCode((byte) 3);// 设置qrcode模块大小
        esc.addStoreQRCodeData("www.smarnet.cc");// 设置qrcode内容
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();

        /* 打印文字 */
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印左对齐
        esc.addText("Completed!\r\n"); // 打印结束
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        // esc.addGeneratePluseAtRealtime(LabelCommand.FOOT.F2, (byte) 8);

        esc.addPrintAndFeedLines((byte) 8);

        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rs;
        try {
            rs = mGpService.sendEscCommand(mPrinterIndex, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void connection() {
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE); // bindService
    }

    public boolean[] getConnectState() {
        boolean[] state = new boolean[GpPrintService.MAX_PRINTER_CNT];
        for (int i = 0; i < GpPrintService.MAX_PRINTER_CNT; i++) {
            state[i] = false;
        }
        for (int i = 0; i < GpPrintService.MAX_PRINTER_CNT; i++) {
            try {
                if (mGpService.getPrinterConnectStatus(i) == GpDevice.STATE_CONNECTED) {
                    state[i] = true;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return state;
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
        long updateTime = App.app.share.getLong("updateTime", 0);
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
        if (App.app.share.getInt("ConnetType", 0) == 1) {// Wifi连接
            if (TextUtils.isEmpty(App.app.share.getString("IP", ""))) {
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
            if (TextUtils.isEmpty(App.app.share.getString("MAC", ""))) {
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
                App.app.share.putLong("updateTime", System.currentTimeMillis());
                App.app.share.commit();
                App.app.showToast("数据同步成功");
                App.app.list.clear();
                App.app.list.addAll(LitePal.findAll(DrugInfoDB.class));
                setSearchBar(this, true);
                setNavTip();
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
            if (App.app.share.getInt("ConnetType", 0) == 0) {
                rb1.setChecked(true);
                et_ip.setHint("蓝牙MAC地址");
                et_ip.setText(App.app.share.getString("MAC", ""));
            } else {
                rb2.setChecked(true);
                et_ip.setHint("IP地址");
                et_ip.setText(App.app.share.getString("IP", ""));
            }
            ((RadioGroup) settingView.findViewById(R.id.radioGroup)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.radioButton1:
                            et_ip.setText(App.app.share.getString("MAC", ""));
                            et_ip.setHint("蓝牙MAC地址");
                            break;
                        case R.id.radioButton2:
                            et_ip.setText(App.app.share.getString("IP", ""));
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
                            App.app.share.putString("MAC", ip);
                            App.app.share.putInt("ConnetType", 0);
                            App.app.share.commit();
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
                        App.app.share.putString("IP", ip);
                        App.app.share.putInt("ConnetType", 1);
                        App.app.share.commit();
                        App.app.showToast("已选择Wifi连接");
                    }
                    setNavTip();
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
