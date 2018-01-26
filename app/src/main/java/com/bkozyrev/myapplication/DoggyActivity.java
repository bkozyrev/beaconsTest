package com.bkozyrev.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class DoggyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doggy);

        Observable.just(true)
                .delay(228, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> {
                    Intent intent = new Intent(this, ScanningActivity.class);
                    intent.putExtra("extra_start_magic_please", true);
                    intent.putExtra("extra_uuid", getIntent().getStringExtra("extra_uuid"));
                    startActivity(intent);
                }, throwable -> {
                    throwable.printStackTrace();
                });
    }
}
