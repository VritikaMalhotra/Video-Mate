package com.example.videomeeting.network;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

//Retrofit uses OkHttp library for HTTP requests.
//Retrofit is the best tool for performing network request in android applications.

public class ApiClient {

    private static Retrofit retrofit = null;

    //This creats and instance of Retrofit Instance.
    //Instannce of retrofit can be created by Retrofit Builder class
    //You have to specify baseurl and converterFactory at the time of retrofit instance creation

    public static Retrofit getClient(){
        if(retrofit == null){
            retrofit =  new Retrofit.Builder()
                    .baseUrl("https://fcm.googleapis.com/fcm/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
