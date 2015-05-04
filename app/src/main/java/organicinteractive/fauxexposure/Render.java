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
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Render extends Activity {

    int imgCount;
    boolean keepPictures = false;
    Calendar c = Calendar.getInstance();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm");
    String curTime = format.format(c.getTime());
    private int width;
    private int height;

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
                subtract();
                break;
            case "Maximum":
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                maxMin(true);
                break;
            case "Minimum":
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                maxMin(false);
            case "Aluminum":
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

        toast("img is " + Integer.toString(width) + " by " + Integer.toString(height));

        for (int i = 1; i < imgCount; i++) {
            BitmapFactory.decodeFile(fp + Integer.toString(i) + ".jpg", options);
            if (options.outWidth != width || options.outHeight != height) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void maxMin(boolean renderMax) {
        //renderMax = true -> get the higher luminance values from the pictures, and vice versa
        String fp = Environment.getExternalStorageDirectory() + "/fauxexposure/";
        String fpOut = null;
        if (renderMax) fpOut = fp + "maximum" + "-" + curTime + ".jpg";
        if (!renderMax) fpOut = fp + "minimum" + "-" + curTime + ".jpg";

        int[] px = new int[width * height];
        int[] pxOut = new int[width * height];
        int red, green, blue, curRed, curGreen, curBlue;
        float luminance, curLuminance;
        int offset;
        Bitmap bm;

        if (!renderMax) {
            //because all ints are initialized to zero, we need to make the output array something
            //else so we can start building the minimums
            red = green = blue = 255;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    offset = y * width + x;
                    pxOut[offset] = 0xff000000 | (red << 16) | (green << 8) | blue;
                }
            }
        }

        for (int imgCounter = 0; imgCounter < imgCount; imgCounter++) {
            bm = BitmapFactory.decodeFile(fp + Integer.toString(imgCounter) + ".jpg");
            bm.getPixels(px, 0, width, 0, 0, width, height);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    offset = y * width + x;

                    blue = px[offset] & 0xff; //blue
                    green = (px[offset] >> 8) & 0xff; //green
                    red = (px[offset] >> 16) & 0xff; //red

                    curBlue = pxOut[offset] & 0xff;
                    curGreen = (pxOut[offset] >> 8) & 0xff;
                    curRed = (pxOut[offset] >> 16) & 0xff;

                    luminance = (float) (0.21 * red + 0.71 * green + 0.07 * blue);
                    curLuminance = (float) (0.21 * curRed + 0.71 * curGreen + 0.07 * curBlue);

                    if (renderMax && luminance > curLuminance) {
                        pxOut[offset] = 0xff000000 | (red << 16) | (green << 8) | blue;
                    } else if (!renderMax && luminance < curLuminance) {
                        pxOut[offset] = 0xff000000 | (red << 16) | (green << 8) | blue;
                    }
                }
            }

            if (!keepPictures) {
                File curPic = new File(fp + Integer.toString(imgCounter) + ".jpg");
                curPic.delete();
            }
        }

        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.setPixels(pxOut, 0, width, 0, 0, width, height);

        try {
            FileOutputStream fos = new FileOutputStream(fpOut);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);

            fos.flush();
            fos.close();

            if (bm != null) {
                bm.recycle();
                bm = null;
            }

        } catch (Exception e) {
            Log.e("MyLog", e.toString());
            toast("shit done failed");
        }
    }

    private void subtract() {
        String fp = Environment.getExternalStorageDirectory() + "/fauxexposure/";
        String fpOut = fp + "subtract" + "-" + curTime + ".jpg";

        int[] px = new int[width * height];
        float[][][] pxRGBAvg = new float[height][width][3];
        float[][][] pxRGBMed = new float[height][width][3];
        int[] pxOut = new int[width * height];
        float red, green, blue;
        int offset;
        Bitmap bm;
        float alpha = 1;
        float div = (1 / imgCount);
        div = 0.05f;

        for (int imgCounter = 0; imgCounter < imgCount; imgCounter++) {
            bm = BitmapFactory.decodeFile(fp + Integer.toString(imgCounter) + ".jpg");
            bm.getPixels(px, 0, width, 0, 0, width, height);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    offset = y * width + x;

                    blue = (px[offset] & 0xff); //blue
                    green = (px[offset] >> 8) & 0xff; //green
                    red = (px[offset] >> 16) & 0xff; //red


                    //The "Jeff McClintock" median filter algorithm
                    pxRGBAvg[y][x][0] += (blue - pxRGBAvg[y][x][0]) * div;
                    if(blue - pxRGBMed[y][x][0] > 0) {
                        pxRGBMed[y][x][0] += pxRGBAvg[y][x][0] * alpha;
                    } else {
                        pxRGBMed[y][x][0] += pxRGBAvg[y][x][0] * alpha * -1;
                    }

                    pxRGBAvg[y][x][1] += (green - pxRGBAvg[y][x][1]) * div;
                    if(green - pxRGBMed[y][x][1] > 0) {
                        pxRGBMed[y][x][1] += pxRGBAvg[y][x][1] * alpha;
                    } else {
                        pxRGBMed[y][x][1] += (pxRGBAvg[y][x][1] * -1) * alpha;
                    }

                    pxRGBAvg[y][x][2] += (red - pxRGBAvg[y][x][2]) * div;
                    if(red - pxRGBMed[y][x][2] > 0) {
                        pxRGBMed[y][x][2] += pxRGBAvg[y][x][2] * alpha;
                    } else {
                        pxRGBMed[y][x][2] += (pxRGBAvg[y][x][2] * -1) * alpha;
                    }

                }
            }

            if (!keepPictures) {
                File curPic = new File(fp + Integer.toString(imgCounter) + ".jpg");
                curPic.delete();
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                offset = y * width + x;

                int b = (int)pxRGBMed[y][x][0];
                int g = (int)pxRGBMed[y][x][1];
                int r = (int)pxRGBMed[y][x][2];

                pxOut[offset] = 0xff000000 | (r << 16) | (g << 8) | b;

            }
        }
        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.setPixels(pxOut, 0, width, 0, 0, width, height);

        try {
            FileOutputStream fos = new FileOutputStream(fpOut);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);


            fos.flush();
            fos.close();

            if (bm != null) {
                bm.recycle();
                bm = null;
            }

        } catch (Exception e) {
            Log.e("MyLog", e.toString());
            toast("shit done failed");
        }
    }

    private void average() {
        //dat O(imgCount * height * width) running time;
        String fp = Environment.getExternalStorageDirectory() + "/fauxexposure/";
        String fpOut = fp + "average" + "-" + curTime + ".jpg";

        int[] px = new int[width * height];
        int[][][] pxRGB = new int[height][width][3];
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

                    pxRGB[y][x][0] += px[offset] & 0xff; //blue
                    pxRGB[y][x][1] += (px[offset] >> 8) & 0xff; //green
                    pxRGB[y][x][2] += (px[offset] >> 16) & 0xff; //red

                }
            }

            if (!keepPictures) {
                File curPic = new File(fp + Integer.toString(imgCounter) + ".jpg");
                curPic.delete();
            }
        }


        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.setPixels(pxOut, 0, width, 0, 0, width, height);

        try {
            FileOutputStream fos = new FileOutputStream(fpOut);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);

            fos.flush();
            fos.close();

            if (bm != null) {
                bm.recycle();
                bm = null;
            }

        } catch (Exception e) {
            Log.e("MyLog", e.toString());
            toast("shit done failed");
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                offset = y * width + x;

                blue = pxRGB[y][x][0] / imgCount;
                green = pxRGB[y][x][1] / imgCount;
                red = pxRGB[y][x][2] / imgCount;

                pxOut[offset] += 0xff000000 | (red << 16) | (green << 8) | blue;

            }
        }

        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.setPixels(pxOut, 0, width, 0, 0, width, height);

        try {
            FileOutputStream fos = new FileOutputStream(fpOut);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);

            fos.flush();
            fos.close();

            if (bm != null) {
                bm.recycle();
                bm = null;
            }

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
