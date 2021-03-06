package uuke.xinfu.wxphoto;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.netcompss.loader.LoadJNI;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import uuke.xinfu.wxphoto.intent.PhotoPickerIntent;
import uuke.xinfu.wxphoto.intent.VideoPickerIntent;

public class CDVWXPhoto extends CordovaPlugin {

    public CallbackContext callbackContext;

    public static final int PERMISSION_DENIED_ERROR = 20;
    protected final static String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE };
    private int maxTotal = 9;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        String NOMEDIA=".nomedia";
        File Folder = new File(Environment.getExternalStorageDirectory() + "/uuke");
        if(Folder.mkdir() || Folder.isDirectory()) {
            File nomediaFile = new File(Environment.getExternalStorageDirectory() + "/uuke/"+ NOMEDIA);
            if(!nomediaFile.exists()){
                try {
                    nomediaFile.createNewFile();
                } catch (Exception e) {
                    Log.i("error", "nomedia failure!") ;
                }
            }
        }
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("pick")) {
            return pick(args, callbackContext);
        }
        else if (action.equals("pickVideo")) {
            return pickVideo(args, callbackContext);
        }
        else if (action.equals("compressVideo")) {
            return compressVideo(args, callbackContext);
        }
        return false;
    }

    protected boolean pick(CordovaArgs args, final CallbackContext callbackContext)  {
        final CDVWXPhoto _this = this;
        try{
            maxTotal = args.getInt(0);
        }catch (JSONException e){

        }
        if(!PermissionHelper.hasPermission(this, permissions[0])) {
            PermissionHelper.requestPermission(this, 0, Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            this.getPicture();
        }
        PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
        r.setKeepCallback(true);
        callbackContext.sendPluginResult(r);


        return true;
    }

    protected boolean pickVideo(CordovaArgs args, final CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        final CDVWXPhoto _this = this;

        if(!PermissionHelper.hasPermission(this, permissions[0])) {
            PermissionHelper.requestPermission(this, 0, Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            this.getVideo();
        }

        PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
        r.setKeepCallback(true);
        callbackContext.sendPluginResult(r);

        return true;
    }
    protected boolean compressVideo(CordovaArgs args, final CallbackContext callbackContext) {
        // GeneralUtils.checkForPermissionsMAndAbove(Main.this, true);
        final LoadJNI vk = new LoadJNI();
        final CDVWXPhoto _this = this;
        final CordovaArgs _args = args;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    String src = _args.getString(0);
                    String name = _args.getString(1);
                    String workFolder = _this.cordova.getActivity().getApplicationContext().getFilesDir().getAbsolutePath();
                    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/uuke";

                    String[] complexCommand = {"ffmpeg", "-y", "-i", src, "-strict", "experimental", "-vf",
                            "scale=iw/2:-1", "-r", "25", "-vcodec", "mpeg4", "-b", "900k", "-ab", "48000", "-ac", "2",
                            "-ar", "22050", path + "/" + name};
                    Context context = _this.cordova.getActivity().getApplicationContext();
                    vk.run(complexCommand, workFolder, context);
                    JSONObject result = new JSONObject();
                    result.put("destUrl", path + "/" + name);
                    _this.callbackContext.success(result);
                    Log.i("test", "ffmpeg4android finished successfully");
                } catch (Throwable e) {
                    Log.e("test", "vk run exception.", e);
                }
            }

        });
        return true;
    }

    public void getPicture() {
        final CDVWXPhoto _this = this;
        PhotoPickerIntent intent = new PhotoPickerIntent(_this.cordova.getActivity());
        intent.setSelectModel(SelectModel.MULTI);
        intent.setShowCarema(true); // 是否显示拍照
        intent.setMaxTotal(maxTotal); // 最多选择照片数量，默认为9
        //intent.setSelectedPaths(imagePaths); // 已选中的照片地址， 用于回显选中状态
        //startActivityForResult(intent, REQUEST_CAMERA_CODE);
        _this.cordova.startActivityForResult((CordovaPlugin) _this, intent, 1);
    }

    public void getVideo() {
        final CDVWXPhoto _this = this;
        VideoPickerIntent intent = new VideoPickerIntent(_this.cordova.getActivity());
        _this.cordova.startActivityForResult((CordovaPlugin) _this, intent, 2);
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        for(int r:grantResults)
        {
            if(r == PackageManager.PERMISSION_DENIED)
            {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }
        this.getPicture();
    }

    /**
     * Called when the camera view exits.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (intent == null)
            return;

        if (requestCode == 1) {
            ArrayList<String> res = intent.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT);
            Boolean isOrigin = intent.getBooleanExtra(PhotoPickerActivity.EXTRA_ORIGIN, false);
            //返回数组
            try {
                JSONArray array = new JSONArray();
                for (int i=0;i<res.size();i++){
                    JSONObject result = new JSONObject();
                    result.put("url", res.get(i));
                    result.put("isOrigin", isOrigin);
                    array.put(i,result);
                }
                this.callbackContext.success(array);
            } catch (JSONException e) {

            }
        } else if (requestCode == 2) {
            Video video = intent.getParcelableExtra("video");
            String coverUrl = intent.getStringExtra("coverUrl");

            try {
                JSONObject result = new JSONObject();
                result.put("url", video.path);
                result.put("coverUrl", coverUrl);
                result.put("duration", video.duration);
                this.callbackContext.success(result);
            } catch (JSONException e) {

            }
        }

    }
}
