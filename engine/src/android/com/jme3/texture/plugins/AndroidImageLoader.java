package com.jme3.texture.plugins;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AndroidImageLoader implements AssetLoader {

    public Object load2(AssetInfo info) throws IOException {
        ByteBuffer bb = BufferUtils.createByteBuffer(1 * 1 * 2);
        bb.put((byte) 0xff).put((byte) 0xff);
        bb.clear();
        return new Image(Format.RGB5A1, 1, 1, bb);
    }

    public Object load(AssetInfo info) throws IOException {
        InputStream in = null;
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            in = info.openStream();
            BitmapFactory.decodeStream(in,null, options);
            float scaleW=(float)options.outWidth /256f;  
            float scaleH=(float)options.outHeight/256f;  
            float scale = 1; //Math.max(scaleW,scaleH);            
            in.close();
            in = null;
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds=false;  
            options.inPurgeable = false;
            options.inSampleSize = (int)FastMath.ceil(scale);
            in = info.openStream();
            bitmap = BitmapFactory.decodeStream(in, null, options);
            if (bitmap == null) {
                throw new IOException("Failed to load image: " + info.getKey().getName());
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Format fmt;

        switch (bitmap.getConfig()) {
            case ALPHA_8:
                fmt = Format.Alpha8;
                break;
            case ARGB_4444:
                fmt = Format.ARGB4444;
                break;
            case ARGB_8888:
                fmt = Format.RGBA8;
                break;
            case RGB_565:
                fmt = Format.RGB565;
                break;
            default:
//                return null;
                throw new IOException("Failed to load image: " + info.getKey().getName());
        }

        if (((TextureKey) info.getKey()).isFlipY()) {
            Bitmap newBitmap = null;
            Matrix flipMat = new Matrix();
            flipMat.preScale(1.0f, -1.0f);
            newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), flipMat, false);
            bitmap.recycle();
            bitmap = newBitmap;

            if (bitmap == null) {
                throw new IOException("Failed to flip image: " + info.getKey().getName());
            }
        }

        Image image = new Image(fmt, width, height, null);
        image.setEfficentData(bitmap);
        return image;
    }
}
