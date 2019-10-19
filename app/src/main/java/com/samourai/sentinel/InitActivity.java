package com.samourai.sentinel;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
//import android.widget.Toast;

import com.samourai.sentinel.network.dojo.Network;
import com.samourai.sentinel.permissions.PermissionsUtil;
import com.samourai.sentinel.util.AppUtil;

public class InitActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        TextView tvStart = (TextView)findViewById(R.id.start);
        tvStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Intent intent = new Intent(InitActivity.this, InsertActivity.class);
                startActivity(intent);

                return false;
            }
        });

        ImageButton ibStart = (ImageButton)findViewById(R.id.startimg);
        ibStart.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Intent intent = new Intent(InitActivity.this, InsertActivity.class);
                startActivity(intent);

                return false;
            }
        });

        if(!PermissionsUtil.getInstance(InitActivity.this).hasPermission(Manifest.permission.CAMERA)) {
            PermissionsUtil.getInstance(InitActivity.this).showRequestPermissionsInfoAlertDialog(PermissionsUtil.CAMERA_PERMISSION_CODE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.init_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_restore) {
            AppUtil.getInstance(InitActivity.this).doRestore();
        }
        if (id == R.id.action_network_init) {
            startActivity(new Intent(this, Network.class));
        }
        else {
            ;
        }

        return super.onOptionsItemSelected(item);
    }

}
