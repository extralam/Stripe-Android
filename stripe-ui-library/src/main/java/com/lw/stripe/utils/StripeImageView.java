package com.lw.stripe.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.lw.stripe.utils.BitmapCacheManager;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * Created by alan on 31/7/16.
 */
public class StripeImageView extends ImageView {

    private DownloadImageTask task;
    private BitmapCacheManager mBitmapCacheManager;

    public StripeImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public StripeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StripeImageView(Context context) {
        super(context);
    }

    public void setUrl(String url) {
        if (mBitmapCacheManager == null) {
            mBitmapCacheManager = new BitmapCacheManager();
        }
        if (isUrl(url)) {
            synchronized (this) {
                if (task != null)
                    task.cancel(true);

                task = (new DownloadImageTask(this));
                task.execute(url);
            }
        }
    }

    private boolean isUrl(String url) {
        return (url.startsWith("http") || url.startsWith("https"));
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        private static final String LOG_E_TAG = "DownloadImageTask";
        private final WeakReference<ImageView> containerImageView;

        public DownloadImageTask(ImageView imageView) {
            this.containerImageView = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String mmURL = params[0];
            Bitmap _output = mBitmapCacheManager.getBitmapFromMemCache(mmURL);
            if (_output == null) {
                try {
                    URL imageURL = new URL(mmURL);
                    InputStream inputStream = imageURL.openStream();
                    _output = BitmapFactory.decodeStream(inputStream);
                    mBitmapCacheManager.addBitmapToMemoryCache(mmURL, _output);
                } catch (Exception e) {
                    Log.e(LOG_E_TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
            return _output;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            ImageView imageView = this.containerImageView.get();
            if (imageView != null && result != null) {
                imageView.setImageBitmap(result);
            }
        }
    }
}
