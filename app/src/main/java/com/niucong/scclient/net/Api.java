package com.niucong.scclient.net;

import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

public interface Api {

    @POST("synData")
    Observable<ReturnData> synData(@Body RequestBody body);
}
