package com.niucong.scclient.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.niucong.scclient.App;
import com.niucong.scclient.DrugActivity;
import com.niucong.scclient.R;
import com.niucong.scclient.db.DrugInfoDB;

import java.util.List;

/**
 * Created by think on 2018/1/2.
 */

public class DrugAdapter extends RecyclerView.Adapter<DrugAdapter.MyViewHolder> {

    private Context context;
    List<DrugInfoDB> sls;

    private int defItem = -1;
    private OnItemListener onItemListener;

    public DrugAdapter(Context context, List<DrugInfoDB> sls) {
        this.context = context;
        this.sls = sls;
    }

    public void setOnItemListener(OnItemListener onItemListener) {
        this.onItemListener = onItemListener;
    }

    public interface OnItemListener {
        void onClick(View v, int pos);

        void onDelete(DrugInfoDB sl);
    }

    public void setDefSelect(int position) {
        this.defItem = position;
        notifyDataSetChanged();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyViewHolder holder = new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_drug, parent, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        final DrugInfoDB sl = sls.get(position);
        holder.tv_code.setText("" + sl.getBarCode());
        holder.tv_num.setText("售价：" + App.app.showPrice(sl.getPrice()));
        holder.tv_name.setText(sl.getName());
        holder.tv_factory.setText(sl.getFactory());

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ((Activity) context).startActivityForResult(new Intent(context, DrugActivity.class)
                        .putExtra("BarCode", sl.getBarCode()), 1);
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return sls.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tv_name, tv_code, tv_factory, tv_num;
        ImageView iv_delete;

        public MyViewHolder(View view) {
            super(view);
            tv_name = (TextView) view.findViewById(R.id.item_store_name);
            tv_code = (TextView) view.findViewById(R.id.item_store_code);
            tv_factory = (TextView) view.findViewById(R.id.item_store_factory);
            tv_num = (TextView) view.findViewById(R.id.item_store_num);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemListener != null) {
                        onItemListener.onClick(v, getLayoutPosition());
                    }
                }
            });
        }
    }
}
