package org.wordpress.android.overlaycamera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.models.SceneItem;


public class OverlayCameraActivity extends ActionBarActivity implements Callback, SwipeInterface
{

    private Camera camera;
    private SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    private ImageView mOverlayView;
    private Canvas canvas;
    private Bitmap bitmap;

    String[] overlays = null;
    int overlayIdx = 0;
    int overlayGroup = 0;

    boolean cameraOn = false;

    private int mColorRed = 0;
    private int mColorGreen = 0;
    private int mColorBlue = 0;

    private int mStoryMode = -1;
    Dialog mDialog;

    private Handler mMediaHandler = new Handler ()
    {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            // FIXME handle response from media capture... send result back to story editor
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        overlayGroup = getIntent().getIntExtra("group", 0);
        overlayIdx = getIntent().getIntExtra("overlay", 0);

        showOverlayCam();
    }

    public void showOverlayCam(){

        mStoryMode = getIntent().getIntExtra("mode",-1);

        mOverlayView = new ImageView(OverlayCameraActivity.this);

        ActivitySwipeDetector swipe = new ActivitySwipeDetector(OverlayCameraActivity.this);
        mOverlayView.setOnTouchListener(swipe);

        mOverlayView.setOnClickListener(new OnClickListener (){
            @Override
            public void onClick(View v) {

                closeOverlay();

            }

        });

        mSurfaceView = new SurfaceView(OverlayCameraActivity.this);
        addContentView(mSurfaceView, new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(OverlayCameraActivity.this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        addContentView(mOverlayView, new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
    }

    private void ScenePickerDialog(Context context){

        mDialog = new Dialog(context);
        mDialog.setContentView(R.layout.list_pick_scene);
        mDialog.setTitle(context.getResources().getString(R.string.pick_scene));

        ListView scenesList = (ListView)mDialog.findViewById(R.id.listView);

        List<String> sceneTitles = Arrays.asList(context.getResources().getStringArray(R.array.scenes));
        List<String> sceneDescriptions = Arrays.asList(context.getResources().getStringArray(R.array.scenes_descriptions));
        List<String> sceneImages = Arrays.asList(context.getResources().getStringArray(R.array.scenes_images));

        ListAdapter scenesAdapter = new ListAdapter(context, sceneTitles, sceneDescriptions, sceneImages, R.layout.row_pick_scene);
        scenesList.setAdapter(scenesAdapter);

        mDialog.show();
    }

    public class ListAdapter extends ArrayAdapter<SceneItem> {

        private List<String> sceneTitles;
        private List<String> sceneDescriptions;
        private List<String> sceneImages;
        private Context context;

        public ListAdapter(Context _context, List<String> _sceneTitles, List<String> _sceneDescriptions, List<String> _sceneImages, int row_pick_scene) {
            super(_context, row_pick_scene);
            this.sceneTitles = _sceneTitles;
            this.sceneDescriptions = _sceneDescriptions;
            this.sceneImages = _sceneImages;
            this.context = _context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.row_pick_scene, null);
            }

                TextView tt1 = (TextView) v.findViewById(R.id.scene_head);
                TextView tt2 = (TextView) v.findViewById(R.id.scene_sub);
                ImageView tt3 = (ImageView) v.findViewById(R.id.scene_image);

                if (tt1 != null) {
                    tt1.setText(sceneTitles.get(position));
                }

                if (tt2 != null) {
                    tt2.setText(sceneDescriptions.get(position));
                }

                if (tt3 != null) {
                    //tt3.setImageDrawable(p.getSceneImage());
                }

            return v;
        }

    }

    public class ScenesAdapter extends BaseAdapter {

        String[] sceneTitles;
        String[] sceneDescriptions;
        String[] sceneImages;
        int rowResource;
        Context context;

        public ScenesAdapter(Context _context, String[] _sceneTitles, String[] _sceneDescriptions, String[] _sceneImages, int _rowResource) {

            this.sceneTitles = _sceneTitles;
            this.sceneDescriptions = _sceneDescriptions;
            this.sceneImages = _sceneImages;
            this.rowResource = _rowResource;
            this.context = _context;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup viewGroup) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);



            //if(convertView!=null) {
                TextView heading;
                TextView subheading;
                ImageView img;

                convertView = inflater.inflate(R.layout.row_pick_scene, null);
                heading = (TextView) convertView.findViewById(R.id.scene_head);
                subheading = (TextView) convertView.findViewById(R.id.scene_sub);
                img = (ImageView) convertView.findViewById(R.id.scene_image);

            //}

            heading.setText("sdfs");
            //holder.subheading.setText(sceneDescriptions[position]);
            //holder.img.setImageDrawable(sceneImages[position]);
            convertView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialog.cancel();
                    overlayGroup = position;
                    overlayIdx = position;
                    showOverlayCam();
                }
            });

            return convertView;
        }

        @Override
        public int getCount() {
            return sceneTitles.length;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }
    }


    private void closeOverlay () {

            if (cameraOn)
            {
                cameraOn = false;

                if (camera != null)
                {
                    camera.stopPreview();
                    camera.release();
                }
            }

            Intent intent=new Intent();
            intent.putExtra("MESSAGE", "Overlay selected");
            setResult(mStoryMode, intent);

            finish();

    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        closeOverlay();
    }


    private void setOverlayImage (int idx)
    {
        try
        {
            String groupPath = "images/overlays/svg/" + overlayGroup;

            //if (overlays == null)
                overlays = getAssets().list(groupPath);

            bitmap = Bitmap.createBitmap(mOverlayView.getWidth(),mOverlayView.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);

            String imgPath = groupPath + '/' + overlays[idx];
            //    SVG svg = SVGParser.getSVGFromAsset(getAssets(), "images/overlays/svg/" + overlays[idx],0xFFFFFF,0xCC0000);

            SVG svg = SVGParser.getSVGFromAsset(getAssets(), imgPath);

            Rect rBounds = new Rect(0,0,mOverlayView.getWidth(),mOverlayView.getHeight());
            Picture p = svg.getPicture();
            canvas.drawPicture(p, rBounds);

            mOverlayView.setImageBitmap( bitmap);
        }
        catch(IOException ex)
        {
            Log.e(BuildConfig.APPLICATION_ID, "error rendering overlay",ex);
            return;
        }

    }


    private Bitmap changeColor(Bitmap src,int pixelRed, int pixelGreen, int pixelBlue){

    	int width = src.getWidth();
    	int height = src.getHeight();

        Bitmap dest = Bitmap.createBitmap(
          width, height, src.getConfig());

        for(int x = 0; x < width; x++){
         for(int y = 0; y < height; y++){
          int pixelColor = src.getPixel(x, y);
          int pixelAlpha = Color.alpha(pixelColor);

	          int newPixel = Color.argb(
	            pixelAlpha, pixelRed, pixelGreen, pixelBlue);

	          dest.setPixel(x, y, newPixel);
         }
        }
        return dest;
       }


    private void takePicture() {
        // TODO Auto-generated method stub
        //  camera.takePicture(shutter, raw, jpeg);
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

        if (camera != null)
        {
            Camera.Parameters p = camera.getParameters();
            p.setPreviewSize(arg2, arg3);
            bitmap = Bitmap.createBitmap(arg2, arg3, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            setOverlayImage (overlayIdx);
            try {
                camera.setPreviewDisplay(arg0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (camera == null && (!cameraOn)&& Camera.getNumberOfCameras() > 0)
        {
            camera = Camera.open();
            cameraOn = true;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        closeOverlay();
    }


    @Override
    public void bottom2top(View v) {
        mColorRed = 255;
        mColorGreen = 255;
        mColorBlue = 255;
        setOverlayImage(overlayIdx);

    }


    @Override
    public void left2right(View v) {
        // TODO Auto-generated method stub
        overlayIdx--;
        if (overlayIdx < 0)
            overlayIdx = overlays.length-1;

        setOverlayImage(overlayIdx);
    }


    @Override
    public void right2left(View v) {

        overlayIdx++;
        if (overlayIdx == overlays.length)
            overlayIdx = 0;

        setOverlayImage(overlayIdx);

    }


    @Override
    public void top2bottom(View v) {
        mColorRed = 0;
        mColorGreen = 0;
        mColorBlue = 0;

        setOverlayImage(overlayIdx);
    }



    ShutterCallback shutter = new ShutterCallback(){

        @Override
        public void onShutter() {
            //no action for the shutter

        }

    };

    PictureCallback raw = new PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //we aren't taking a picture here
        }

    };

    PictureCallback jpeg = new PictureCallback(){

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //we aren't taking a picture here

        }

    };
}


/*
float sx = svg.getLimits().width() / ((float)mOverlayView.getWidth());
float sy = svg.getLimits().height() / ((float)mOverlayView.getHeight());
canvas.scale(1/sx, 1/sy);
PictureDrawable d = svg.createPictureDrawable();
d.setBounds(new Rect(0,0,mOverlayView.getWidth(),mOverlayView.getHeight()));
//d.setColorFilter(0xffff0000, Mode.MULTIPLY);
int iColor = Color.parseColor("#FFFFFF");
int red = (iColor & 0xFF0000) / 0xFFFF;
int green = (iColor & 0xFF00) / 0xFF;
int blue = iColor & 0xFF;
float[] matrix = { 0, 0, 0, 0, red
                 , 0, 0, 0, 0, green
                 , 0, 0, 0, 0, blue
                 , 0, 0, 0, 1, 0 };
ColorFilter colorFilter = new ColorMatrixColorFilter(matrix);
d.setColorFilter(colorFilter);
d.draw(canvas);
*/