package organicinteractive.fauxexposure;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.widget.ImageView.ScaleType;
import static org.opencv.core.CvType.CV_32FC3;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;

public class Render extends Activity {

    String fp = Environment.getExternalStorageDirectory() + "/fauxexposure/";
    String fpOut;
    int imgCount;
    boolean keepPictures = false;
    Calendar c = Calendar.getInstance();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm");
    String curTime = format.format(c.getTime());
    private int width;
    private int height;

    Button contrastBtn, smoothBtn, erodeBtn;
    ImageView imageView;
    Bitmap displayBM;
    Mat m;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        try {
            System.loadLibrary("opencv_java");
        } catch (UnsatisfiedLinkError e) {
            Log.v("fe_debug", "catching");
            toast("libloadfail: " + e.getMessage());
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        Bundle extras = getIntent().getExtras();
        String exposureType = extras.getString(Main.exposureTypeMsg);
        imgCount = extras.getInt(Frame.imgCountMsg);
//        if (!checkImages()) {
//            Log.v("fe_debug", "not all images same size, aborting");
//            toast("Not all images are the same size!"); //die
//            Intent intent = new Intent(Intent.ACTION_MAIN);
//            intent.addCategory(Intent.CATEGORY_HOME);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        //Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(fp + "00.jpg", options);
        width = options.outWidth;
        height = options.outHeight;
        dbg(Integer.toString(width) + " " + Integer.toString(height));

        switch (exposureType) {
            case "Average":
                dbg("using average");
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                average();
                break;
            case "Subtract":
                dbg("using subtract");
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                subtract();
                break;
            case "Maximum":
                dbg("using max");
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                maxMin(true);
                break;
            case "Minimum":
                dbg("using min");
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                maxMin(false);
                break;
            case "Median (OpenCV)":
                dbg("using runningMedian");
                toast(exposureType + " for " + Integer.toString(imgCount) + " frames.");
                runningMedian();
                break;
            default:
                dbg("hitting default");
                toast("wtf how did you get here");
                break;
        }
        dbg("t1");
        dbg("t2");
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                findViewById(R.id.progressBar).setVisibility(View.GONE);
                displayBM = BitmapFactory.decodeFile(fpOut);
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                displayBM =  Bitmap.createBitmap(displayBM, 0, 0, displayBM.getWidth(), displayBM.getHeight(), matrix, true);
                dbg("t3");
                imageView = (ImageView)findViewById(R.id.imageView);
                imageView.setScaleType(ScaleType.CENTER_INSIDE);
                imageView.setImageBitmap(displayBM);
                dbg("t4");

                dbg("t5");
                m = new Mat();
                //temp mat for bitmap -> mat and back conversions
                //imageView can only display bitmaps, and OpenCV can only manipulate Mats
                Bitmap bmp32 = displayBM.copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bmp32, m); //creates a CV_8UC3 in m
                dbg("t6");

            }
        }, 2500);
        dbg("6.5");

//        smoothBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Mat m2 = new Mat();
//                dbg("7");
//                m.convertTo(m, CV_8UC3);
//                org.opencv.imgproc.Imgproc.bilateralFilter(m, m2, 15, 80, 80);
//                dbg("8");
//                Utils.matToBitmap(m2, displayBM);
//                dbg("9");
//                imageView = (ImageView) findViewById(R.id.imageView);
//                imageView.setImageBitmap(displayBM);
//            }
//        });


    }


    private boolean checkImages() {
        //Make sure all the images are the same size. Checks against 0.jpg (first image in sequence)

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        //Returns null, sizes are in the options variable
        BitmapFactory.decodeFile(fp + "00.jpg", options);
        width = options.outWidth;
        height = options.outHeight;

        if (width == 0 || height == 0) {
            toast("unable to load pictures, dying");
            return false;
        }

        toast("img is " + Integer.toString(width) + " by " + Integer.toString(height));

        for (int i = 1; i < imgCount; i++) {
            BitmapFactory.decodeFile(fp + String.format("%02d", Integer.toString(i)) + ".jpg", options);
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
            bm = BitmapFactory.decodeFile(fp + String.format("%02d", imgCounter) + ".jpg");
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
                File curPic = new File(fp + String.format("%02d", imgCounter) + ".jpg");
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
        fpOut = fp + "subtract" + "-" + curTime + ".jpg";

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
            bm = BitmapFactory.decodeFile(fp + String.format("%02d", imgCounter) + ".jpg");
            bm.getPixels(px, 0, width, 0, 0, width, height);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    offset = y * width + x;

                    blue = (px[offset] & 0xff); //blue
                    green = (px[offset] >> 8) & 0xff; //green
                    red = (px[offset] >> 16) & 0xff; //red


                    //The "Jeff McClintock" median filter algorithm
                    pxRGBAvg[y][x][0] += (blue - pxRGBAvg[y][x][0]) * div;
                    if (blue - pxRGBMed[y][x][0] > 0) {
                        pxRGBMed[y][x][0] += pxRGBAvg[y][x][0] * alpha;
                    } else {
                        pxRGBMed[y][x][0] += pxRGBAvg[y][x][0] * alpha * -1;
                    }

                    pxRGBAvg[y][x][1] += (green - pxRGBAvg[y][x][1]) * div;
                    if (green - pxRGBMed[y][x][1] > 0) {
                        pxRGBMed[y][x][1] += pxRGBAvg[y][x][1] * alpha;
                    } else {
                        pxRGBMed[y][x][1] += (pxRGBAvg[y][x][1] * -1) * alpha;
                    }

                    pxRGBAvg[y][x][2] += (red - pxRGBAvg[y][x][2]) * div;
                    if (red - pxRGBMed[y][x][2] > 0) {
                        pxRGBMed[y][x][2] += pxRGBAvg[y][x][2] * alpha;
                    } else {
                        pxRGBMed[y][x][2] += (pxRGBAvg[y][x][2] * -1) * alpha;
                    }

                }
            }

            if (!keepPictures) {
                File curPic = new File(fp + String.format("%02d", imgCounter) + ".jpg");
                curPic.delete();
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                offset = y * width + x;

                int b = (int) pxRGBMed[y][x][0];
                int g = (int) pxRGBMed[y][x][1];
                int r = (int) pxRGBMed[y][x][2];

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
        dbg("avg 0");
        fpOut = fp + "average" + "-" + curTime + ".jpg";

        int[] px = new int[width * height];
        int[][][] pxRGB = new int[height][width][3];
        int[] pxOut = new int[width * height];
        int red, green, blue;
        int offset;
        Bitmap bm;
        dbg("avg 1");
        for (int imgCounter = 0; imgCounter < imgCount; imgCounter++) {
            dbg("avg 2 " + Integer.toString(imgCounter));
            bm = BitmapFactory.decodeFile(fp + String.format("%02d", imgCounter) + ".jpg");
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
                File curPic = new File(fp + String.format("%02d", imgCounter) + ".jpg");
                curPic.delete();
            }
        }

        dbg("avg 3");

        bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.setPixels(pxOut, 0, width, 0, 0, width, height);

        dbg("avg 4");
        try {
            FileOutputStream fos = new FileOutputStream(fpOut);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);

            dbg("avg 5");
            fos.flush();
            fos.close();

            if (bm != null) {
                bm.recycle();
                bm = null;
            }

        } catch (Exception e) {
            dbg("avg + " + e.toString());
            toast("shit done failed");
        }
        dbg("avg 6");

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

        dbg("avg 7");
        try {
            FileOutputStream fos = new FileOutputStream(fpOut);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, fos);

            dbg("avg 8");
            fos.flush();
            fos.close();

            if (bm != null) {
                bm.recycle();
                bm = null;
            }

        } catch (Exception e) {
            dbg("avg + " + e.toString());
            toast("shit done failed");
        }

    }

    private void runningMedian() {

        dbg("got to 2");

        fpOut = fp + "runningMedian" + "-" + curTime + ".jpg";
        Mat tmp = imread(fp + "00.jpg");
        Mat outImg = Mat.zeros(tmp.size(), CV_32FC3);
        outImg.convertTo(outImg, CV_32FC3);
        Mat curImg = new Mat();

        dbg("got to 2.5");

        String fname = "";
        for (int i = 0; i < imgCount; i++) {
            fname = fp + String.format("%02d", i) + ".jpg";
            dbg(fname);
            curImg = imread(fname);
            curImg.convertTo(curImg, CV_32FC3);
            Imgproc.accumulateWeighted(curImg, outImg, 0.01);
            if (!keepPictures) {
                File curPic = new File(fname);
                curPic.delete();
            }
        }

        dbg("got to 3");

        double min;
        double max;
        tmp = outImg.reshape(1);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(tmp);
        max = mmr.maxVal;
        min = mmr.minVal;
        dbg("got to 4");

        //imwrite(fp + "runningMedian" + "-" + curTime + ".jpg", outImg);

        dbg(Double.toString(max) + " // " + Double.toString(min));
        double scale = 255.0 / (max - min);
        dbg(Double.toString(scale));

        outImg.convertTo(outImg, CV_8UC3, scale);
        imwrite(fpOut, outImg);

    }


    private void toast(String out) {
        //cuz I'm real lazy like that
        Toast.makeText(getApplicationContext(), out, Toast.LENGTH_SHORT).show();
    }

    private void dbg(String msg) {
        Log.v("fe_debug", msg);
    }
}
