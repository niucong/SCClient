package com.niucong.scclient;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.niucong.scclient.db.DrugInfoDB;
import com.niucong.scclient.util.CnToSpell;

import org.litepal.LitePal;

import java.text.SimpleDateFormat;

//import com.umeng.analytics.MobclickAgent;

public class DrugActivity extends BasicActivity {
    private String TAG = "EnterActivity";

    private TextView tv_sell_price;
    private EditText et_code, et_price;
    private AutoCompleteTextView et_name, et_factory;
    private Button btn_send;

    private DrugInfoDB di;// 药品信息

    private long barCode;

    private SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drug);
//        MobclickAgent.onEvent(this, "1");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("添加库存");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setSearchBar(this, true);
        setView();

        btn_send.setOnClickListener(this);

        barCode = getIntent().getLongExtra("BarCode", 0);
        if (barCode > 0) {
            searchDrug(barCode + "");
        }
    }

    private void setView() {
        et_code = (EditText) findViewById(R.id.enter_code);
        et_name = (AutoCompleteTextView) findViewById(R.id.enter_name);
        et_factory = (AutoCompleteTextView) findViewById(R.id.enter_factory);

        tv_sell_price = (TextView) findViewById(R.id.enter_sell_price);
        et_price = (EditText) findViewById(R.id.enter_price);

        btn_send = (Button) findViewById(R.id.enter_btn);

        SearchAdapter searchAdapter = new SearchAdapter(this, App.app.list);
        et_name.setAdapter(searchAdapter);
        et_factory.setAdapter(searchAdapter);

        et_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (isManualInput) {
                    searchType = 1;
                }
                if (str.contains("\n")) {
                    str = str.replace("\n", "");
                    et_name.setText(str);
                    et_name.setSelection(str.length());
                }
            }
        });

        et_factory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (isManualInput) {
                    searchType = 2;
                }
                if (str.contains("\n")) {
                    str = str.replace("\n", "");
                    et_factory.setText(str);
                    et_factory.setSelection(str.length());
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.enter_btn:
                String str_code = et_code.getText().toString();
                String str_name = et_name.getText().toString();
                String str_factory = et_factory.getText().toString();
                String str_price = et_price.getText().toString();

                if (TextUtils.isEmpty(str_name) || TextUtils.isEmpty(str_code)) {
                    Snackbar.make(btn_send, "药品名称和条码不能为空", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return;
                }
                addDrugInfo(str_code, str_name, str_factory, str_price);
                break;
        }
    }

    /**
     * 保存药品
     *
     * @param str_code
     * @param str_name
     * @param str_factory
     * @param str_price
     */
    private void addDrugInfo(String str_code, String str_name, String str_factory, String str_price) {
        if (di == null) {
            di = new DrugInfoDB();
        }

        try {
            di.setBarCode(Long.valueOf(str_code));
            di.setName(str_name);
            di.setFactory(str_factory);
            di.setPrice(App.app.savePrice(str_price));
            di.setNamePY(CnToSpell.getPinYin(str_name));
            di.setNamePYF(CnToSpell.getPinYinHeadChar(str_name));
            di.setUpdateTime(System.currentTimeMillis());

            if (di.getPrice() <= 0) {
                Snackbar.make(btn_send, "销售价格必须大于0", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
        } catch (Exception e) {
            Snackbar.make(btn_send, "药品信息输入错误", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        boolean flag = false;
        for (DrugInfoDB drugInfo : App.app.list) {
            if (drugInfo.getBarCode() == di.getBarCode()) {
                flag = true;
                break;
            }
        }
        di.saveOrUpdate();
        if (!flag) {
            App.app.list.add(di);
            setSearchBar(this, true);
            SearchAdapter searchAdapter = new SearchAdapter(this, App.app.list);
            et_name.setAdapter(searchAdapter);
        }
        clearInput();
        et_search.requestFocus();
        Snackbar.make(btn_send, "入库成功", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    @Override
    protected boolean searchDrug(String result) {
        Log.d(TAG, "searchDrug code=" + result);
        if (TextUtils.isEmpty(result)) {
//            Snackbar.make(btn_send, "条形码输入错误", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
            return false;
        }
        isManualInput = false;
        if (searchType == 0) {
            clearInput();
        }
        di = LitePal.where("barCode = ?", result).findFirst(DrugInfoDB.class);
        if (searchType == 0) {
            et_code.setText(result);
        }
        et_search.setText("");
        if (di != null) {
            if (searchType == 0 || searchType == 2) {
                et_factory.setText(di.getFactory());
            }
            if (searchType == 0 || searchType == 1) {
                et_name.setText(di.getName());
            }
            if (searchType == 0) {
                String price = App.app.showPrice(di.getPrice());
                tv_sell_price.setText("销售价格：" + price);
                et_price.setText(price);
            }
        }
        isManualInput = true;
        return true;
    }

    private void clearInput() {
        et_code.setText("");
        et_name.setText("");
        et_factory.setText("");
        tv_sell_price.setText("销售价格：0.0");
        et_price.setText("");

        di = null;
    }
}
