package organicinteractive.fauxexposure;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Frame extends Activity {

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Button exposeBtn, rotateBtn, lockExposureBtn;
    Camera camera;

    private boolean cameraConfigured = false;
    boolean landscape = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame);

        Intent intent = getIntent();
        String message = intent.getStringExtra(Main.exposureSettings);

        exposeBtn = (Button) findViewById(R.id.exposeBtn);
        rotateBtn = (Button) findViewById(R.id.rotateBtn);
        lockExposureBtn = (Button) findViewById(R.id.lockExposureBtn);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceCallback);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        rotateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (getResources().getConfiguration().orientation) {
                    case Configuration.ORIENTATION_PORTRAIT:
                        toast("switching to landscape");
                        landscape = true;
                        break;
                    case Configuration.ORIENTATION_LANDSCAPE:
                        toast("switching to portrait");
                        landscape = false;
                        break;
                }
            }
        });


        exposeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "expose button pressed", Toast.LENGTH_SHORT).show();
                releaseCameraAndPreview();

            }
        });

        lockExposureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setAutoExposureLock(!parameters.getAutoExposureLock());
                camera.setParameters(parameters);
            }
        });
    }

    private void toast(String out) {
        //cuz I'm real lazy like that
        Toast.makeText(getApplicationContext(), out, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        safeCameraOpen();
        startPreview();
    }

    private void startPreview() {
        if (cameraConfigured && camera != null) {
            camera.startPreview();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_frame, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void safeCameraOpen() {
       try {
            releaseCameraAndPreview();
            camera = Camera.open();
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }
    }

    private void releaseCameraAndPreview() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private void initPreview(int width, int height) {
        if (camera != null && surfaceHolder.getSurface() != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (Throwable t) {
                Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t);
                Toast.makeText(Frame.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters = camera.getParameters();
                //Camera.Size size = getBestPreviewSize(width, height, parameters);

//                if (size != null) {
//                    parameters.setPreviewSize(size.width, size.height);
                camera.setParameters(parameters);
                cameraConfigured = true;
//                }
            }
            cameraConfigured = true;
        }
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        //shitty algorithm that makes things stretched out
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return (result);
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            camera.stopPreview();

            Camera.Parameters parameters = camera.getParameters();

//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);

            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            //always keep these things:
            initPreview(width, height);
            camera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            //graciously give the camera back so other apps can use it
            releaseCameraAndPreview();
        }
    };
}