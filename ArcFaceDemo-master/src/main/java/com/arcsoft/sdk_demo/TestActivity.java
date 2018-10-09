package com.arcsoft.sdk_demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.ageestimation.ASAE_FSDKAge;
import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.ageestimation.ASAE_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKGender;
import com.arcsoft.genderestimation.ASGE_FSDKVersion;
import com.arcsoft.sdk_demo.database.Report;
import com.guo.android_extend.image.ImageConverter;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView.OnCameraListener;
import com.liyu.sqlitetoexcel.SQLiteToExcel;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;
import com.raizlabs.android.dbflow.sql.language.Delete;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.arcsoft.facedetection.AFD_FSDKEngine.AFD_FOC_0;
import static com.arcsoft.facetracking.AFT_FSDKEngine.AFT_FOC_0;
import static com.arcsoft.facetracking.AFT_FSDKEngine.AFT_FOC_180;
import static com.arcsoft.facetracking.AFT_FSDKEngine.AFT_FOC_270;
import static com.arcsoft.facetracking.AFT_FSDKEngine.AFT_FOC_90;

/**
 * Created by gqj3375 on 2017/4/28.
 */

public class TestActivity extends Activity implements View.OnClickListener {
	private static final int FILE_CODE = 0x01;
	private final String TAG = this.getClass().getSimpleName();

	private int mWidth, mHeight, mFormat;

	AFT_FSDKVersion version = new AFT_FSDKVersion();
	AFT_FSDKEngine engine = new AFT_FSDKEngine();
	List<AFT_FSDKFace> result = new ArrayList<>();

	ASAE_FSDKVersion mAgeVersion = new ASAE_FSDKVersion();
	ASAE_FSDKEngine mAgeEngine = new ASAE_FSDKEngine();
	List<ASAE_FSDKAge> ages = new ArrayList<>();

	ASGE_FSDKVersion mGenderVersion = new ASGE_FSDKVersion();
	ASGE_FSDKEngine mGenderEngine = new ASGE_FSDKEngine();
	List<ASGE_FSDKGender> genders = new ArrayList<>();
	
	byte[] mImageNV21 = null;
	AFT_FSDKFace mAFT_FSDKFace = null;
	Handler mHandler;
	boolean isPostted = false;


	private File mSelectedFile = new File("/sdcard/test/");

	private String mReportPath = null;

	private Button mChoseButton;

	private Button mStartButton;

	private Button mExportButton;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		mWidth = 1280;
		mHeight = 960;
		mFormat = ImageFormat.NV21;
		mHandler = new Handler();

		setContentView(R.layout.activity_test);
		mStartButton = (Button) findViewById(R.id.start_button);
		mChoseButton = (Button) findViewById(R.id.chose_button);
		mExportButton = (Button) findViewById(R.id.export_button);
		mStartButton.setOnClickListener(this);
		mChoseButton.setOnClickListener(this);
		mExportButton.setOnClickListener(this);
		mStartButton.setEnabled(mSelectedFile != null);
		mChoseButton.setEnabled(false);
		mExportButton.setEnabled(false);
		AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
		Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
		err = engine.AFT_FSDK_GetVersion(version);
		Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

		ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceDB.appid, FaceDB.age_key);
		Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
		error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
		Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());

		ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceDB.appid, FaceDB.gender_key);
		Log.d(TAG, "ASGE_FSDK_InitgGenderEngine =" + error1.getCode());
		error1 = mGenderEngine.ASGE_FSDK_GetVersion(mGenderVersion);
		Log.d(TAG, "ASGE_FSDK_GetVersion:" + mGenderVersion.toString() + "," + error1.getCode());


		/*FileWriter fw = null;
		try {
			File file = new File(getExternalFilesDir(""), "report.txt");
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();
			mReportPath = file.getAbsolutePath();
			fw = new FileWriter(mReportPath);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write("FileName\t" +
					"Left\t" +
					"Top\t" +
					"Right\t" +
					"Bottom\t" +
					"Age\t" +
					"Gender\t" +
					"Degree\t\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}*/

	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
		Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

		ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
		Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());

		ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
		Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.start_button) {
			doWork();
		} else if (view.getId() == R.id.chose_button) {
			choseDirectory();
		} else if (view.getId() == R.id.export_button) {
			exportReport();
		}
	}

	private void exportReport() {
		new SQLiteToExcel
				.Builder(this)
				.setDataBase(getDatabasePath("face.db").getPath()) //必须。 小提示：内部数据库可以通过 context.getDatabasePath("internal.db").getPath() 获取。
				//.setTables(table1, table2) //可选, 如果不设置，则默认导出全部表。
				//.setPath(outoutPath) //可选, 如果不设置，默认输出路径为 app ExternalFilesDir。
				//.setFileName("test.xls") //可选, 如果不设置，输出的文件名为 xxx.db.xls。
				//.setEncryptKey("1234567") //可选，可对导出的文件进行加密。
				//.setProtectKey("9876543") //可选，可对导出的表格进行只读的保护。
				.start(new SQLiteToExcel.ExportListener() {

					@Override
					public void onStart() {
						Log.d(TAG, "xls start");
					}

					@Override
					public void onCompleted(String filePath) {
						Log.d(TAG, "xls complete");
						Toast.makeText(TestActivity.this, "测试完成，xls文件存放于/sdcard/Android/data/本应用目录", Toast.LENGTH_LONG).show();
					}

					@Override
					public void onError(Exception e) {
						Log.d(TAG, "", e);
					}
				}); // 或者使用 .start() 同步方法。
	}

	private void choseDirectory() {
		// This always works
		Intent i = new Intent(this, FilePickerActivity.class);
		// This works if you defined the intent filter
		// Intent i = new Intent(Intent.ACTION_GET_CONTENT);

		// Set these depending on your use case. These are the defaults.
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
		i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
		i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

		// Configure initial directory by specifying a String.
		// You could specify a String like "/storage/emulated/0/", but that can
		// dangerous. Always use Android's API calls to get paths to the SD-card or
		// internal memory.
		i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

		startActivityForResult(i, FILE_CODE);
	}

	private void doWork() {
		if (mSelectedFile == null) {
			Toast.makeText(this, "please select directory first!", Toast.LENGTH_SHORT).show();
			return;
		}
        Delete.table(Report.class);
		new Thread(new Runnable() {
            @Override
            public void run() {
                handleFile(mSelectedFile.getAbsolutePath());
                Log.d(TAG, "handle file complete");
                mExportButton.post(new Runnable() {
                    @Override
                    public void run() {
                        mExportButton.setEnabled(true);
                    }
                });
            }
        }).start();
		/*Disposable disposable = Flowable.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer integer) {
                handleFile(mSelectedFile.getAbsolutePath());
                return new Object();
            }
        }).subscribeOn(Schedulers.io()).subscribe();*/

	}


	private void handleFile(final String path) {
	    Log.d(TAG, "handleFile " + path);
		File file = new File(path);
		if (!file.exists()) return;
		if (file.isDirectory()) {
			for (File path1 : file.listFiles()) {
                handleFile(path1.getAbsolutePath());
            }
			return;
		}
		try {
			List<ASAE_FSDKFace> face1 = new ArrayList<>();
			List<ASGE_FSDKFace> face2 = new ArrayList<>();
			Bitmap bitmap = Application.decodeImage(path);
			if (bitmap == null) return;
			mWidth = bitmap.getWidth();
			mHeight = bitmap.getHeight();
			byte[] data = new byte[mWidth * mHeight * 3 / 2];
			ImageConverter convert = new ImageConverter();
			convert.initial(mWidth, mHeight, ImageConverter.CP_PAF_NV21);
			if (convert.convert(bitmap, data)) {
				Log.d(TAG, "convert ok!");
			}
			convert.destroy();
			mImageNV21 = data.clone();
            result.clear();
			AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, result);
			Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
			Log.d(TAG, "Face=" + result.size());
			for (AFT_FSDKFace face : result) {
				Log.d(TAG, "Face:" + face.toString());
			}
			if (result.size() == 0) {
                Report report = new Report();
                report.fileName = path;
                report.age = 0;
                report.gender = "0";
                report.degree = 0;
                report.rect = "Rect(0,0,0,0)";
                report.insert();
                return;
            }
			mAFT_FSDKFace = result.get(0).clone();
			face1.clear();
			face2.clear();
			face1.add(new ASAE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
			face2.add(new ASGE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
			ASAE_FSDKError error1 = mAgeEngine.ASAE_FSDK_AgeEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face1, ages);
			ASGE_FSDKError error2 = mGenderEngine.ASGE_FSDK_GenderEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face2, genders);
			Log.d(TAG, "ASAE_FSDK_AgeEstimation_Image:" + error1.getCode() + ",ASGE_FSDK_GenderEstimation_Image:" + error2.getCode());
			Log.d(TAG, "age:" + ages.get(0).getAge() + ",gender:" + genders.get(0).getGender());
			final String age = ages.get(0).getAge() == 0 ? "年龄未知" : ages.get(0).getAge() + "岁";
			final String gender = genders.get(0).getGender() == -1 ? "性别未知" : (genders.get(0).getGender() == 0 ? "男" : "女");
			Report report = new Report();
			report.fileName = path;
			report.age = ages.get(0).getAge();
			report.gender = gender;
			report.degree = mapDegree(mAFT_FSDKFace.getDegree());
			report.rect = mAFT_FSDKFace.getRect().toString();
			report.insert();
		} catch (Exception e) {
			Log.d(TAG, "", e);
		}

	}

    private int mapDegree(int degree) {
	    switch (degree) {
            case AFT_FOC_0:
                return 0;
            case AFT_FOC_90:
                return 90;
            case AFT_FOC_270:
                return 270;
            case AFT_FOC_180:
                return 180;
        }
        return -1;
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
			// Use the provided utility method to parse the result
			List<Uri> files = Utils.getSelectedFilesFromResult(data);
			for (Uri uri: files) {
				File file = Utils.getFileForUri(uri);
				mSelectedFile = file;
				assert mSelectedFile != null;
				mStartButton.setEnabled(true);
			}
		}
	}
}
