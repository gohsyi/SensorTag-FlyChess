package sensortag;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.project3c.flychess.MainActivity;
import com.project3c.flychess.R;


public class SensorTagActivity extends AppCompatActivity implements OnStatusListener {

    private Fragment mCurrentFragment;
    private FragmentManager mFragmentManager;
    private SwipeRefreshLayout mSwipeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensortag);

        // exit if the device doesn't have BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_SHORT).show();
            finish();
        }

        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        mSwipeContainer.setEnabled(false);

        // load ScanFragment
        mFragmentManager = getSupportFragmentManager();
        mCurrentFragment = ScanFragment.newInstance();
        mFragmentManager.beginTransaction().replace(R.id.container, mCurrentFragment).commit();
    }

    @Override
    /*
    * Click on the SensorTag MAC address and switch to its information
    * Jump to flychess game
    * */
    public void onListFragmentInteraction(String address) {
        // deliver the address to flychess
        Intent intent = new Intent(SensorTagActivity.this, MainActivity.class);
        intent.putExtra("address", address.substring("[SensorTag Device] ".length()));
        startActivity(intent);
    }

    @Override
    public void onShowProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwipeContainer.setRefreshing(true);
            }
        });
    }

    @Override
    public void onHideProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwipeContainer.setRefreshing(false);
            }
        });
    }
}
