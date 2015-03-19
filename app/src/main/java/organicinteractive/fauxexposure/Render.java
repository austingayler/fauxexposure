package organicinteractive.fauxexposure;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;


public class Render extends Activity {

    int imgCount;
    private int width;
    private int height;
    boolean keepFiles = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render);

        Bundle extras = getIntent().getExtras();

        String exposureType = extras.getString(Main.exposureTypeMsg);
        imgCount = extras.getInt(Frame.imgCountMsg);

        if (!checkImages()) {
            toast("Not all images are the same size!"); //die
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        switch (exposureType) {
            case "Average":
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                average();
                break;
            case "Subtract":
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                break;
            case "Swag two hundred million":
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                break;
            default:
                toast("wtf how did you get here");
                break;
        }

    }

    private boolean checkImages() {
        //Make sure all the images are the same size. Checks against 0.jpg (first image in sequence)
        String fp = Environment.getExternalStorageDirectory() + "/fauxexposure/";

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        //Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(fp + "0.jpg", options);
        width = options.outWidth;
        height = options.outHeight;

//        toast("img is " + Integer.toString(width) + " by " + Integer.toString(height));

        for (int i = 1; i < imgCount; i++) {
            BitmapFactory.decodeFile(fp + Integer.toString(i) + ".jpg", options);
            if (options.outWidth != width || options.outHeight != height) {
                return false;
            }
        }
        return true;
    }

    private void average() {
        //dat O(imgCount * height * width) running time;
        String fp = Environment.getExternalStorageDirectory() + "/fauxexposure/";
        String fpOut = fp + "out.jpg";

        int[] px = new int[width * height];
        int[] pxOut = new int[width * height];
        int red, green, blue;
        int offset;
        Bitmap bm;

        for (int imgCounter = 0; imgCounter < imgCount; imgCounter++) {
            bm = BitmapFactory.decodeFile(fp + Integer.toString(imgCounter) + ".jpg");
            bm.getPixels(px, 0, width, 0, 0, width, height);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    offset = y * width + x;

                    blue = px[offset] & 0xff / imgCount;
                    green = ((px[offset] >> 8) & 0xff) / imgCount;
                    red = ((px[offset] >> 16) & 0xff) / imgCount;

                    pxOut[offset] += 0xff000000 | (red << 16) | (green << 8) | blue;

                }
            }
            if(!keepFiles) {
                File curPhoto = new File(fp + Integer.toString(imgCounter) + ".jpg");
                curPhoto.delete();
            }
        }

        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.setPixels(pxOut, 0, width, 0, 0, width, height);

        try {
            FileOutputStream fos = new FileOutputStream(fpOut);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);

            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e("MyLog", e.toString());
            toast("shit done failed");
        }

    }

    private void toast(String out) {
        //cuz I'm real lazy like that
        Toast.makeText(getApplicationContext(), out, Toast.LENGTH_SHORT).show();
    }
}
