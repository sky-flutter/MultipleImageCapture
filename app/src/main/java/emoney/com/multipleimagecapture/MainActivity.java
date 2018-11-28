package emoney.com.multipleimagecapture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    ArrayList<String> listCaptureImage;
    LinearLayout llImage;
    private CameraView mCameraView;
    private Handler mBackgroundHandler;
    private String TAG = "MainActivity";
    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Toast.makeText(cameraView.getContext(), "Picture taken", Toast.LENGTH_SHORT)
                    .show();
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            System.currentTimeMillis() + "_picture.jpg");
                    try (OutputStream os = new FileOutputStream(file)) {
                        os.write(data);
                        os.close();
                        listCaptureImage.add(file.getPath());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showCaptureImage();
                            }
                        });
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    }
                    // Ignore
                }
            });
        }
    };

    private void showCaptureImage() {
        if (listCaptureImage.size() > 0) {
            llImage.removeAllViews();
            for (int i = 0; i < listCaptureImage.size();
                 i++) {
                final String strPath = listCaptureImage.get(i);
                View view = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.item_image_capture, null, false);
                ImageButton imageButton = view.findViewById(R.id.ib_item_capture_clear);
                imageButton.setTag(i);
                final int finalI = i;
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listCaptureImage.remove(finalI);
                        showCaptureImage();
                    }
                });
                ImageView ivImage = view.findViewById(R.id.iv_capture_image);
                Glide.with(this).load(strPath).into(ivImage);
                llImage.addView(view);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = (CameraView) findViewById(R.id.camera);
        llImage = findViewById(R.id.ll_main_image);
        listCaptureImage = new ArrayList<>();
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }
    }

    public void onCaptureImage(View view) {
        if (mCameraView != null) {
            mCameraView.takePicture();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            mBackgroundHandler.getLooper().quitSafely();
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Not granted",
                            Toast.LENGTH_SHORT).show();
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }
}
