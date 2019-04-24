package top.zibin.luban;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Responsible for starting compress and managing active and cached resources.
 */
class Engine {
  private InputStreamProvider srcImg;
  private File tagImg;
  private int srcWidth;
  private int srcHeight;
  private boolean focusAlpha;

  Engine(InputStreamProvider srcImg, File tagImg, boolean focusAlpha) throws IOException {
    this.tagImg = tagImg;
    this.srcImg = srcImg;
    this.focusAlpha = focusAlpha;

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    options.inSampleSize = 1;

    BitmapFactory.decodeStream(srcImg.open(), null, options);
    this.srcWidth = options.outWidth;
    this.srcHeight = options.outHeight;
  }

  /**
   *  主要是用来计算图片的宽和高的对比，来制定缩放的比例
   * @return
   */
  private int computeSize() {
    //宽和高只能是偶数
    srcWidth = srcWidth % 2 == 1 ? srcWidth + 1 : srcWidth;
    srcHeight = srcHeight % 2 == 1 ? srcHeight + 1 : srcHeight;

    //得到宽和高最大的值
    int longSide = Math.max(srcWidth, srcHeight);
    //各到宽和高最小的值
    int shortSide = Math.min(srcWidth, srcHeight);
    //计算出图版的宽和高的比例
    float scale = ((float) shortSide / longSide);
    //宽和高相差比例不是很大的情况
    if (scale <= 1 && scale > 0.5625) {
      if (longSide < 1664) {
        return 1;
      } else if (longSide < 4990) {
        return 2;
      } else if (longSide > 4990 && longSide < 10240) {
        return 4;
      } else {
        return longSide / 1280 == 0 ? 1 : longSide / 1280;
      }
    }
    //宽和高误差相近一倍的情况
    else if (scale <= 0.5625 && scale > 0.5) {
      return longSide / 1280 == 0 ? 1 : longSide / 1280;
    }
    //相差小于一倍的情况
    else {
      return (int) Math.ceil(longSide / (1280.0 / scale));
    }
  }

  private Bitmap rotatingImage(Bitmap bitmap, int angle) {
    Matrix matrix = new Matrix();

    matrix.postRotate(angle);

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
  }

  File compress() throws IOException {
    BitmapFactory.Options options = new BitmapFactory.Options();
    //设置压缩的尺寸，值为2的幂次方，如果为1表示不压缩
    options.inSampleSize = computeSize();

    Bitmap tagBitmap = BitmapFactory.decodeStream(srcImg.open(), null, options);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    if (Checker.SINGLE.isJPG(srcImg.open())) {
      tagBitmap = rotatingImage(tagBitmap, Checker.SINGLE.getOrientation(srcImg.open()));
    }
    tagBitmap.compress(focusAlpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 60, stream);
    tagBitmap.recycle();

    FileOutputStream fos = new FileOutputStream(tagImg);
    fos.write(stream.toByteArray());
    fos.flush();
    fos.close();
    stream.close();

    return tagImg;
  }
}