package com.niucong.scclient.net;

import java.util.Map;

import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

public interface Api {

    @FormUrlEncoded
    @POST("updateDrug")
    Observable<ReturnData> signList(@FieldMap Map<String, String> fields);

    @FormUrlEncoded
    @POST("uploadDrug")
    Observable<ReturnData> vacate(@FieldMap Map<String, String> fields);
}
