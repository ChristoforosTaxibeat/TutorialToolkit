package com.example.christoforoskolovos.tutorialtoolkit;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.viewmagnifier.Magnifier;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.fab).setOnClickListener(this);
        findViewById(R.id.view1).setOnClickListener(this);
        findViewById(R.id.view2).setOnClickListener(this);
        findViewById(R.id.view3).setOnClickListener(this);
        findViewById(R.id.view4).setOnClickListener(this);
        findViewById(R.id.parent).setOnClickListener(this);

    }

    @Override
    public void onClick(final View view) {
        float scale = 2f;
        if (view.getId() == R.id.parent || view.getId() == R.id.view4)
            scale = 0.5f;
        else if( view.getId() == R.id.view2)
            scale = 3f;

        Magnifier mag = new Magnifier(MainActivity.this, view, scale, "Lorem ipsum dolor sit amet, pri saperet adipisci convenire et, vel stet paulo populo id.");
        mag.setStateListener(new Magnifier.stateListener() {
            @Override
            public void onExpandStarted() {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onExpandEnded() {

            }

            @Override
            public void onMinimizeStarted() {

            }

            @Override
            public void onMinimizeEnded() {
                view.setVisibility(View.VISIBLE);
            }
        });

        mag.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

            }
        });
        mag.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {

            }
        });
        mag.show();
    }
}
