package organicinteractive.fauxexposure;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

@SuppressWarnings("deprecation")
public class Frame extends Activity {
    public final static String imgCountMsg = "com.organicinteractive.fauxexposure.imgCountMsg";
    int imgCount;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Button exposeBtn, rotateBtn, lockExposureBtn;
    Camera camera;
    boolean landscape = false;
    String exposureType;
    int exposureTime;
    View globalView;
    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            camera.stopPreview();

            Camera.Parameters parameters = camera.getParameters();

//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);

            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            initPreview(width, height);
            camera.startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            //graciously give the camera back so other apps can use it
            releaseCameraAndPreview();
        }
    };
    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {

        }
    };
    private boolean cameraConfigured = false;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        imgCount = 0;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame);

        Bundle extras = getIntent().getExtras();

        exposureType = extras.getString(Main.exposureTypeMsg);
        exposureTime = extras.getInt(Main.numSecondsMsg, 10);

        exposeBtn = (Button) findViewById(R.id.exposeBtn);
//        rotateBtn = (Button) findViewById(R.id.rotateBtn);
        lockExposureBtn = (Button) findViewById(R.id.lockExposureBtn);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceCallback);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

//        rotateBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                switch (getResources().getConfiguration().orientation) {
//                    case Configuration.ORIENTATION_PORTRAIT:
//                        toast("switching to landscape");
//                        landscape = true;
//                        break;
//                    case Configuration.ORIENTATION_LANDSCAPE:
//                        toast("switching to portrait");
//                        landscape = false;
//                        break;
//                }
//            }
//        });


        exposeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //3 second timer to eliminate camera shake
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        startTime = System.currentTimeMillis();
                        globalView = v;
                        camera.takePicture(shutterCallback, null, pictureCallback);
                    }
                }, 3000);

            }
        });

        lockExposureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setAutoExposureLock(!parameters.getAutoExposureLock());
                toast("auto exposure set to " + parameters.getAutoExposureLock());
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
        imgCount = 0;
        super.onResume();
        safeCameraOpen();
        startPreview();
    }

    private void startPreview() {
        if (cameraConfigured && camera != null) {
            camera.startPreview();
        }
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
    }    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            long timeElapsed = System.currentTimeMillis() - startTime;
            if (timeElapsed > exposureTime * 1000) {
//                toast("all done!");
                Intent intent = new Intent(globalView.getContext(), Render.class);
                intent.putExtra(imgCountMsg, imgCount);
                intent.putExtra(Main.exposureTypeMsg, exposureType);
                startActivity(intent);
                return;
            }

            String fName = String.format("%02d", imgCount) + ".jpg";
            String fDir = Environment.getExternalStorageDirectory() + "/fauxexposure/";
            File sddir = new File(fDir);
            if (!sddir.mkdirs()) { //make sure dir exists before writing to it
                if (sddir.exists()) {
                } else {
                    toast("error making folder yo");
                    return;
                }
            }
            File photo = new File(fDir, fName);
            if (photo.exists()) {
                photo.delete();
            }
            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                fos.write(data);
                fos.close();
//                toast("pic " + Integer.toString(imgCount) + " been written to " + photo.getPath());
                imgCount++;
                startPreview(); //update camera preview so we can get another picture
                camera.takePicture(shutterCallback, null, pictureCallback); //take another picture once current one has been written
            } catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
                toast("somethin done went wrong yo");
            }
        }
    };


}