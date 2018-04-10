package com.android.volley.toolbox;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.android.volley.VolleyLog;
import com.android.volley.toolbox.DiskLruCache.Snapshot;
import com.android.volley.toolbox.ImageLoader.ImageCache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.LruCache;

public class VolleyImageCache implements ImageCache{
	
	private int maxSize = (int) (Runtime.getRuntime().maxMemory() / 8);

	private DiskLruCache mDiskLruCache;

	// 磁盘缓存大小 10MB
	private static final int DISKMAXSIZE = 10 * 1024 * 1024;
	
	private static final boolean DEBUG = VolleyLog.DEBUG;

	private LruCache<String, Bitmap> mLruCache = null;

	public VolleyImageCache(Context context) {
		mLruCache = new LruCache<String, Bitmap>(maxSize) {

			@Override
			protected int sizeOf(String key, Bitmap value) {
				// 返回bitmap占用的内存大小
				return value.getRowBytes() * value.getHeight();
			}

		};
		try {
			mDiskLruCache = DiskLruCache.open(new File(getAbsDiskDir(context.getApplicationContext(), "volley_image_cache")),
					getVersionCode(context)
					, 1, DISKMAXSIZE);
			if (DEBUG) {
				VolleyLog.v("Volley Image Cache from %s", mDiskLruCache.getDirectory()+"$");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String getAbsDiskDir(Context context, String childPath) {
		String dirPath = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			dirPath = context.getExternalFilesDir(childPath).getAbsolutePath();
		} else {
			dirPath = context.getFilesDir().getAbsolutePath() + File.separator + childPath;
		}
		return dirPath;
	}
	
	private int getVersionCode(Context context) {
		try {
			PackageManager manager = context.getPackageManager();
			PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			return 0;
		}
	}

	@Override
	public Bitmap getBitmap(String url) {
		if (mLruCache.get(url) != null) {
            // 从LruCache缓存中取
            if (DEBUG) {
				VolleyLog.v("Volley Image Cache from %s", "LruCahce");
			}
            return mLruCache.get(url);
        } else {
            String key =url.substring(url.lastIndexOf(File.separator));
            try {
                if (mDiskLruCache.get(key) != null) {
                    // 从DiskLruCahce取
                    Snapshot snapshot = mDiskLruCache.get(key);
                    Bitmap bitmap = null;
                    if (snapshot != null) {
                        bitmap = BitmapFactory.decodeStream(snapshot.getInputStream(0));
                        // 存入LruCache缓存
                        mLruCache.put(url, bitmap);
                        if (DEBUG) {
            				VolleyLog.v("Volley Image Cache from %s", "DiskLruCahce");
            			}
                    }
                    return bitmap;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		// 存入LruCache缓存
        mLruCache.put(url, bitmap);
        // 判断是否存在DiskLruCache缓存，若没有存入
        String key = url.substring(url.lastIndexOf(File.separator));
        try {
            if (mDiskLruCache.get(key) == null) {
                DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                if (editor != null) {
                    OutputStream outputStream = editor.newOutputStream(0);
                    //注意必須 必須使用png 格式 否則背景會變黑
                    if (bitmap.compress(CompressFormat.PNG,100, outputStream)) {
                        editor.commit();
                    } else {
                        editor.abort();
                    }
                }
                mDiskLruCache.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
}
