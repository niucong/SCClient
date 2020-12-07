package com.niucong.scclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.niucong.scclient.adapter.DrugAdapter;
import com.niucong.scclient.db.DrugInfoDB;

import org.litepal.LitePal;

import java.util.List;

public class DrugListActivity extends BasicActivity {

    private RecyclerView mRecyclerView;

    private List<DrugInfoDB> mDatas;
    private DrugAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drug_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("查看库存");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setSearchBar(this, true);

        mRecyclerView = (RecyclerView) findViewById(R.id.drug_rv);
        setData();
    }

    private void setData() {
        mDatas = LitePal.findAll(DrugInfoDB.class);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter = new DrugAdapter(this, mDatas));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));
        mRecyclerView.requestFocus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (App.app.refresh) {
            setData();
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected boolean searchDrug(String result) {
        if (TextUtils.isEmpty(result)) {
//            Snackbar.make(tv_warn, "条形码输入错误", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
            return false;
        }
        long code = Long.valueOf(result);
        for (int i = 0; i < mDatas.size(); i++) {
            if (mDatas.get(i).getBarCode() == code) {
                mRecyclerView.scrollToPosition(i);
                mAdapter.setDefSelect(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_select, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_add:
                startActivityForResult(new Intent(this, DrugActivity.class), 1);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
