package com.niucong.scclient.db;

import android.os.Parcel;
import android.os.Parcelable;

import org.litepal.crud.LitePalSupport;

public class DrugInfoDB extends LitePalSupport implements Parcelable {

    private int id;
    private long barCode;
    private String name;
    private String factory;
    private String namePY;
    private String namePYF;
    private long updateTime;
    private long price;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getBarCode() {
        return barCode;
    }

    public void setBarCode(long barCode) {
        this.barCode = barCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public String getNamePY() {
        return namePY;
    }

    public void setNamePY(String namePY) {
        this.namePY = namePY;
    }

    public String getNamePYF() {
        return namePYF;
    }

    public void setNamePYF(String namePYF) {
        this.namePYF = namePYF;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeLong(this.barCode);
        dest.writeString(this.name);
        dest.writeString(this.factory);
        dest.writeString(this.namePY);
        dest.writeString(this.namePYF);
        dest.writeLong(this.updateTime);
        dest.writeLong(this.price);
    }

    public DrugInfoDB() {
    }

    protected DrugInfoDB(Parcel in) {
        this.id = in.readInt();
        this.barCode = in.readLong();
        this.name = in.readString();
        this.factory = in.readString();
        this.namePY = in.readString();
        this.namePYF = in.readString();
        this.updateTime = in.readLong();
        this.price = in.readLong();
    }

    public static final Creator<DrugInfoDB> CREATOR = new Creator<DrugInfoDB>() {
        @Override
        public DrugInfoDB createFromParcel(Parcel source) {
            return new DrugInfoDB(source);
        }

        @Override
        public DrugInfoDB[] newArray(int size) {
            return new DrugInfoDB[size];
        }
    };
}
