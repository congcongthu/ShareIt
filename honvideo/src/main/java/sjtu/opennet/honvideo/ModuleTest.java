package sjtu.opennet.honvideo;

import android.content.Context;

import sjtu.opennet.hon.Textile;

public class ModuleTest {
    //private String testVideo;
    private static final String TAG = "";

    public static void run(Context context, String videoPath){

        String testPath = FileUtil.getAppExternalPath(context, "test");
        VideoUploadHelper vHelper = new VideoUploadHelper(context, videoPath);
        vHelper.publishMeta();
    }

}
