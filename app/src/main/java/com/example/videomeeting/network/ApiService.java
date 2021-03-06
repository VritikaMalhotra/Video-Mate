package com.example.videomeeting.network;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

//This interface contains methods that represents possible API calls.
//Each method needs a BaseUrl end point annoation that represents HTTP  methods like GET , POST et.
//THe return type of each of these methods is the  instance of the call class.

public interface ApiService {

    @POST("send") //The argument denotes end point of base URL(from ApiClient)
    Call<String> sendRemoteMessage(
            @HeaderMap HashMap<String, String> headers,
            @Body String remoteBody
            );
    //Above header and body are used due to the usage of POST i.e to create contents of the packte which will be transferred.
}
