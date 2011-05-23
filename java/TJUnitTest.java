/*
 * Copyright (C)2011 D. R. Commander.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the libjpeg-turbo Project nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS",
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This program tests the various code paths in the TurboJPEG JNI Wrapper
 */

import java.io.*;
import java.util.*;
import java.awt.image.*;
import javax.imageio.*;
import org.libjpegturbo.turbojpeg.*;

public class TJUnitTest {

  private static final String classname =
    new TJUnitTest().getClass().getName();

  private static void usage() {
    System.out.println("\nUSAGE: java " + classname + " [options]\n");
    System.out.println("Options:\n");
    System.out.println("-yuv = test YUV encoding/decoding support\n");
    System.out.println("-bi = test BufferedImage support\n");
    System.exit(1);
  }

  private final static String subNameLong[] = {
    "4:4:4", "4:2:2", "4:2:0", "GRAY", "4:4:0"
  };
  private final static String subName[] = {
    "444", "422", "420", "GRAY", "440"
  };

  private final static String pixFormatStr[] = {
    "RGB", "BGR", "RGBX", "BGRX", "XBGR", "XRGB", "Grayscale"
  };
  private final static int biType[] = {
    0, BufferedImage.TYPE_3BYTE_BGR, BufferedImage.TYPE_INT_BGR,
    BufferedImage.TYPE_INT_RGB, 0, 0, BufferedImage.TYPE_BYTE_GRAY
  };

  private final static int _3byteFormats[] = {
    TJ.PF_RGB, TJ.PF_BGR
  };
  private final static int _3byteFormatsBI[] = {
    TJ.PF_BGR
  };
  private final static int _4byteFormats[] = {
    TJ.PF_RGBX, TJ.PF_BGRX, TJ.PF_XBGR, TJ.PF_XRGB
  };
  private final static int _4byteFormatsBI[] = {
    TJ.PF_RGBX, TJ.PF_BGRX
  };
  private final static int onlyGray[] = {
    TJ.PF_GRAY
  };
  private final static int onlyRGB[] = {
    TJ.PF_RGB
  };

  private final static int YUVENCODE = 1;
  private final static int YUVDECODE = 2;
  private static int yuv = 0;
  private static boolean bi = false;

  private static int exitStatus = 0;

  private static double getTime() {
    return (double)System.nanoTime() / 1.0e9;
  }

  private final static byte pixels[][] = {
    {0, (byte)255, 0},
    {(byte)255, 0, (byte)255},
    {0, (byte)255, (byte)255},
    {(byte)255, 0, 0},
    {(byte)255, (byte)255, 0},
    {0, 0, (byte)255},
    {(byte)255, (byte)255, (byte)255},
    {0, 0, 0},
    {0, 0, (byte)255}
  };

  private static void initBuf(byte[] buf, int w, int pitch, int h, int pf,
    int flags) throws Exception {
    int roffset = TJ.getRedOffset(pf);
    int goffset = TJ.getGreenOffset(pf);
    int boffset = TJ.getBlueOffset(pf);
    int ps = TJ.getPixelSize(pf);
    int index, row, col, halfway = 16;

    Arrays.fill(buf, (byte)0);
    if(pf == TJ.PF_GRAY) {
      for(row = 0; row < h; row++) {
        for(col = 0; col < w; col++) {
          if((flags & TJ.FLAG_BOTTOMUP) != 0)
            index = pitch * (h - row - 1) + col;
          else index = pitch * row + col;
          if(((row / 8) + (col / 8)) % 2 == 0)
            buf[index] = (row < halfway) ? (byte)255 : 0;
          else buf[index] = (row < halfway) ? 76 : (byte)226;
        }
      }
      return;
    }
    for(row = 0; row < h; row++) {
      for(col = 0; col < w; col++) {
        if((flags & TJ.FLAG_BOTTOMUP) != 0)
          index = pitch * (h - row - 1) + col * ps;
        else index = pitch * row + col * ps;
        if(((row / 8) + (col / 8)) % 2 == 0) {
          if(row < halfway) {
            buf[index + roffset] = (byte)255;
            buf[index + goffset] = (byte)255;
            buf[index + boffset] = (byte)255;
          }
        }
        else {
          buf[index + roffset] = (byte)255;
          if(row >= halfway) buf[index + goffset] = (byte)255;
        }
      }
    }
  }

  private static void initIntBuf(int[] buf, int w, int pitch, int h, int pf,
    int flags) throws Exception {
    int rshift = TJ.getRedOffset(pf) * 8;
    int gshift = TJ.getGreenOffset(pf) * 8;
    int bshift = TJ.getBlueOffset(pf) * 8;
    int index, row, col, halfway = 16;

    Arrays.fill(buf, 0);
    for(row = 0; row < h; row++) {
      for(col = 0; col < w; col++) {
        if((flags & TJ.FLAG_BOTTOMUP) != 0)
          index = pitch * (h - row - 1) + col;
        else index = pitch * row + col;
        if(((row / 8) + (col / 8)) % 2 == 0) {
          if(row < halfway) {
            buf[index] |= (255 << rshift);
            buf[index] |= (255 << gshift);
            buf[index] |= (255 << bshift);
          }
        }
        else {
          buf[index] |= (255 << rshift);
          if(row >= halfway) buf[index] |= (255 << gshift);
        }
      }
    }
  }

  private static void initImg(BufferedImage img, int pf, int flags)
    throws Exception {
    WritableRaster wr = img.getRaster();
    int imgtype = img.getType();
    if(imgtype == BufferedImage.TYPE_INT_RGB
      || imgtype == BufferedImage.TYPE_INT_BGR) {
      SinglePixelPackedSampleModel sm =
        (SinglePixelPackedSampleModel)img.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
      int[] buf = db.getData();
      initIntBuf(buf, img.getWidth(), pitch, img.getHeight(), pf, flags);
    }
    else {
      ComponentSampleModel sm = (ComponentSampleModel)img.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
      byte[] buf = db.getData();
      initBuf(buf, img.getWidth(), pitch, img.getHeight(), pf, flags);
    }
  }

  private static void checkVal(int row, int col, int v, String vname, int cv)
    throws Exception {
    v = (v < 0) ? v + 256 : v;
    if(v < cv - 1 || v > cv + 1) {
      throw new Exception("\nComp. " + vname + " at " + row + "," + col
        + " should be " + cv + ", not " + v + "\n");
    }
  }

  private static void checkVal0(int row, int col, int v, String vname)
    throws Exception {
    v = (v < 0) ? v + 256 : v;
    if(v > 1) {
      throw new Exception("\nComp. " + vname + " at " + row + "," + col
        + " should be 0, not " + v + "\n");
    }
  }

  private static void checkVal255(int row, int col, int v, String vname)
    throws Exception {
    v = (v < 0) ? v + 256 : v;
    if(v < 254) {
      throw new Exception("\nComp. " + vname + " at " + row + "," + col
        + " should be 255, not " + v + "\n");
    }
  }

  private static int checkBuf(byte[] buf, int w, int pitch, int h, int pf,
    int subsamp, TJScalingFactor sf, int flags) throws Exception {
    int roffset = TJ.getRedOffset(pf);
    int goffset = TJ.getGreenOffset(pf);
    int boffset = TJ.getBlueOffset(pf);
    int ps = TJ.getPixelSize(pf);
    int index, row, col, retval = 1;
    int halfway = 16 * sf.getNum() / sf.getDenom();
    int blockSize = 8 * sf.getNum() / sf.getDenom();

    try {
      for(row = 0; row < halfway; row++) {
        for(col = 0; col < w; col++) {
          if((flags & TJ.FLAG_BOTTOMUP) != 0)
            index = pitch * (h - row - 1) + col * ps;
          else index = pitch * row + col * ps;
          byte r = buf[index + roffset];
          byte g = buf[index + goffset];
          byte b = buf[index + boffset];
          if(((row / blockSize) + (col / blockSize)) % 2 == 0) {
            if(row < halfway) {
              checkVal255(row, col, r, "R");
              checkVal255(row, col, g, "G");
              checkVal255(row, col, b, "B");
            }
            else {
              checkVal0(row, col, r, "R");
              checkVal0(row, col, g, "G");
              checkVal0(row, col, b, "B");
            }
          }
          else {
            if(subsamp == TJ.SAMP_GRAY) {
              if(row < halfway) {
                checkVal(row, col, r, "R", 76);
                checkVal(row, col, g, "G", 76);
                checkVal(row, col, b, "B", 76);
              }
              else {
                checkVal(row, col, r, "R", 226);
                checkVal(row, col, g, "G", 226);
                checkVal(row, col, b, "B", 226);
              }
            }
            else {
              checkVal255(row, col, r, "R");
              if(row < halfway) {
                checkVal0(row, col, g, "G");
              }
              else {
                checkVal255(row, col, g, "G");
              }
              checkVal0(row, col, b, "B");							
            }
          }
        }
      }
    }
    catch(Exception e) {
      System.out.println(e);
      retval = 0;
    }

    if(retval == 0) {
      System.out.print("\n");
      for(row = 0; row < h; row++) {
        for(col = 0; col < w; col++) {
          int r = buf[pitch * row + col * ps + roffset];
          int g = buf[pitch * row + col * ps + goffset];
          int b = buf[pitch * row + col * ps + boffset];
          if(r < 0) r += 256;  if(g < 0) g += 256;  if(b < 0) b += 256;
          System.out.format("%3d/%3d/%3d ", r, g, b);
        }
        System.out.print("\n");
      }
    }
    return retval;
  }

  private static int checkIntBuf(int[] buf, int w, int pitch, int h, int pf,
    int subsamp, TJScalingFactor sf, int flags) throws Exception {
    int rshift = TJ.getRedOffset(pf) * 8;
    int gshift = TJ.getGreenOffset(pf) * 8;
    int bshift = TJ.getBlueOffset(pf) * 8;
    int index, row, col, retval = 1;
    int halfway = 16 * sf.getNum() / sf.getDenom();
    int blockSize = 8 * sf.getNum() / sf.getDenom();

    try {
      for(row = 0; row < halfway; row++) {
        for(col = 0; col < w; col++) {
          if((flags & TJ.FLAG_BOTTOMUP) != 0)
            index = pitch * (h - row - 1) + col;
          else index = pitch * row + col;
          int r = (buf[index] >> rshift) & 0xFF;
          int g = (buf[index] >> gshift) & 0xFF;
          int b = (buf[index] >> bshift) & 0xFF;
          if(((row / blockSize) + (col / blockSize)) % 2 == 0) {
            if(row < halfway) {
              checkVal255(row, col, r, "R");
              checkVal255(row, col, g, "G");
              checkVal255(row, col, b, "B");
            }
            else {
              checkVal0(row, col, r, "R");
              checkVal0(row, col, g, "G");
              checkVal0(row, col, b, "B");
            }
          }
          else {
            if(subsamp == TJ.SAMP_GRAY) {
              if(row < halfway) {
                checkVal(row, col, r, "R", 76);
                checkVal(row, col, g, "G", 76);
                checkVal(row, col, b, "B", 76);
              }
              else {
                checkVal(row, col, r, "R", 226);
                checkVal(row, col, g, "G", 226);
                checkVal(row, col, b, "B", 226);
              }
            }
            else {
              checkVal255(row, col, r, "R");
              if(row < halfway) {
                checkVal0(row, col, g, "G");
              }
              else {
                checkVal255(row, col, g, "G");
              }
              checkVal0(row, col, b, "B");
            }
          }
        }
      }
    }
    catch(Exception e) {
      System.out.println(e);
      retval = 0;
    }

    if(retval == 0) {
      System.out.print("\n");
      for(row = 0; row < h; row++) {
        for(col = 0; col < w; col++) {
          int r = (buf[pitch * row + col] >> rshift) & 0xFF;
          int g = (buf[pitch * row + col] >> gshift) & 0xFF;
          int b = (buf[pitch * row + col] >> bshift) & 0xFF;
          if(r < 0) r += 256;  if(g < 0) g += 256;  if(b < 0) b += 256;
          System.out.format("%3d/%3d/%3d ", r, g, b);
        }
        System.out.print("\n");
      }
    }
    return retval;
  }

  private static int checkImg(BufferedImage img, int pf,
    int subsamp, TJScalingFactor sf, int flags) throws Exception {
    WritableRaster wr = img.getRaster();
    int imgtype = img.getType();
    if(imgtype == BufferedImage.TYPE_INT_RGB
      || imgtype == BufferedImage.TYPE_INT_BGR) {
      SinglePixelPackedSampleModel sm =
        (SinglePixelPackedSampleModel)img.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
      int[] buf = db.getData();
      return checkIntBuf(buf, img.getWidth(), pitch, img.getHeight(), pf,
        subsamp, sf, flags);
    }
    else {
      ComponentSampleModel sm = (ComponentSampleModel)img.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
      byte[] buf = db.getData();
      return checkBuf(buf, img.getWidth(), pitch, img.getHeight(), pf, subsamp,
        sf, flags);
    }
  }

  private static int PAD(int v, int p) {
    return ((v + (p) - 1) & (~((p) - 1)));
  }

  private static int checkBufYUV(byte[] buf, int size, int w, int h,
    int subsamp) throws Exception {
    int i, j;
    int hsf = TJ.getMCUWidth(subsamp)/8, vsf = TJ.getMCUHeight(subsamp)/8;
    int pw = PAD(w, hsf), ph = PAD(h, vsf);
    int cw = pw / hsf, ch = ph / vsf;
    int ypitch = PAD(pw, 4), uvpitch = PAD(cw, 4);
    int retval = 1;
    int correctsize = ypitch * ph
      + (subsamp == TJ.SAMP_GRAY ? 0 : uvpitch * ch * 2);

    try {
      if(size != correctsize)
        throw new Exception("\nIncorrect size " + size + ".  Should be "
          + correctsize);

      for(i = 0; i < 16; i++) {
        for(j = 0; j < pw; j++) {
          byte y = buf[ypitch * i + j];
          if(((i / 8) + (j / 8)) % 2 == 0) checkVal255(i, j, y, "Y");
          else checkVal(i, j, y, "Y", 76);
        }
      }
      for(i = 16; i < ph; i++) {
        for(j = 0; j < pw; j++) {
          byte y = buf[ypitch * i + j];
          if(((i / 8) + (j / 8)) % 2 == 0) checkVal0(i, j, y, "Y");
          else checkVal(i, j, y, "Y", 226);
        }
      }
      if(subsamp != TJ.SAMP_GRAY) {
        for(i = 0; i < 16 / vsf; i++) {
          for(j = 0; j < cw; j++) {
            byte u = buf[ypitch * ph + (uvpitch * i + j)],
              v = buf[ypitch * ph + uvpitch * ch + (uvpitch * i + j)];
            if(((i * vsf / 8) + (j * hsf / 8)) % 2 == 0) {
              checkVal(i, j, u, "U", 128);  checkVal(i, j, v, "V", 128);
            }
            else {
              checkVal(i, j, u, "U", 85);  checkVal255(i, j, v, "V");
            }
          }
        }
        for(i = 16 / vsf; i < ch; i++) {
          for(j = 0; j < cw; j++) {
            byte u = buf[ypitch * ph + (uvpitch * i + j)],
              v = buf[ypitch * ph + uvpitch * ch + (uvpitch * i + j)];
            if(((i * vsf / 8) + (j * hsf / 8)) % 2 == 0) {
              checkVal(i, j, u, "U", 128);  checkVal(i, j, v, "V", 128);
            }
            else {
              checkVal0(i, j, u, "U");  checkVal(i, j, v, "V", 149);
            }
          }
        }
      }
    }
    catch(Exception e) {
      System.out.println(e);
      retval = 0;
    }

    if(retval == 0) {
      for(i = 0; i < ph; i++) {
        for(j = 0; j < pw; j++) {
          int y = buf[ypitch * i + j];
          if(y < 0) y += 256;
          System.out.format("%3d ", y);
        }
        System.out.print("\n");
      }
      System.out.print("\n");
      for(i = 0; i < ch; i++) {
        for(j = 0; j < cw; j++) {
          int u = buf[ypitch * ph + (uvpitch * i + j)];
          if(u < 0) u += 256;
          System.out.format("%3d ", u);
        }
        System.out.print("\n");
      }
      System.out.print("\n");
      for(i = 0; i < ch; i++) {
        for(j = 0; j < cw; j++) {
          int v = buf[ypitch * ph + uvpitch * ch + (uvpitch * i + j)];
          if(v < 0) v += 256;
          System.out.format("%3d ", v);
        }
        System.out.print("\n");
      }
      System.out.print("\n");
    }

    return retval;
  }

  private static void writeJPEG(byte[] jpegBuf, int jpegBufSize,
    String filename) throws Exception {
    File file = new File(filename);
    FileOutputStream fos = new FileOutputStream(file);
    fos.write(jpegBuf, 0, jpegBufSize);
    fos.close();
  }

  private static int compTest(TJCompressor tjc, byte[] dstBuf, int w,
    int h, int pf, String baseName, int subsamp, int jpegQual,
    int flags) throws Exception {
    String tempstr;
    byte[] srcBuf = null;
    BufferedImage img = null;
    String pfStr;
    double t;
    int size = 0, ps = TJ.getPixelSize(pf);

    pfStr = pixFormatStr[pf];

    System.out.print(pfStr + " ");
    if((flags & TJ.FLAG_BOTTOMUP) != 0) System.out.print("Bottom-Up");
    else System.out.print("Top-Down ");
    System.out.print(" -> " + subNameLong[subsamp] + " ");
    if(yuv == YUVENCODE) System.out.print("YUV ... ");
    else System.out.print("Q" + jpegQual + " ... ");

    if(bi) {
      img = new BufferedImage(w, h, biType[pf]);
      initImg(img, pf, flags);
      tempstr = baseName + "_enc_" + pfStr + "_"
        + (((flags & TJ.FLAG_BOTTOMUP) != 0) ? "BU" : "TD") + "_"
        + subName[subsamp] + "_Q" + jpegQual + ".png";
      File file = new File(tempstr);
      ImageIO.write(img, "png", file);
    }
    else {
      srcBuf = new byte[w * h * ps + 1];
      initBuf(srcBuf, w, w * ps, h, pf, flags);
    }
    Arrays.fill(dstBuf, (byte)0);

    t = getTime();
    tjc.setSubsamp(subsamp);
    tjc.setJPEGQuality(jpegQual);
    if(bi) {
      if(yuv == YUVENCODE) tjc.encodeYUV(img, dstBuf, flags);
      else tjc.compress(img, dstBuf, flags);
    }
    else {
      tjc.setSourceImage(srcBuf, w, 0, h, pf);
      if(yuv == YUVENCODE) tjc.encodeYUV(dstBuf, flags);
      else tjc.compress(dstBuf, flags);
    }
    size = tjc.getCompressedSize();
    t = getTime() - t;

    if(yuv == YUVENCODE)
      tempstr = baseName + "_enc_" + pfStr + "_"
        + (((flags & TJ.FLAG_BOTTOMUP) != 0) ? "BU" : "TD") + "_"
        + subName[subsamp] + ".yuv";
    else
      tempstr = baseName + "_enc_" + pfStr + "_"
        + (((flags & TJ.FLAG_BOTTOMUP) != 0) ? "BU" : "TD") + "_"
        + subName[subsamp] + "_Q" + jpegQual + ".jpg";
    writeJPEG(dstBuf, size, tempstr);

    if(yuv == YUVENCODE) {
      if(checkBufYUV(dstBuf, size, w, h, subsamp) == 1)
        System.out.print("Passed.");
      else {
        System.out.print("FAILED!");  exitStatus = -1;
      }
    }
    else System.out.print("Done.");
    System.out.format("  %.6f ms\n", t * 1000.);
    System.out.println("  Result in " + tempstr);

    return size;
  }

  private static void decompTest(TJDecompressor tjd, byte[] jpegBuf,
    int jpegSize, int w, int h, int pf, String baseName, int subsamp,
    int flags, TJScalingFactor sf) throws Exception {
    String pfStr, tempstr;
    double t;
    int scaledWidth = sf.getScaled(w);
    int scaledHeight = sf.getScaled(h);
    int temp1, temp2;
    BufferedImage img = null;
    byte[] dstBuf = null;

    if(yuv == YUVENCODE) return;

    pfStr = pixFormatStr[pf];
    System.out.print("JPEG -> ");
    if(yuv == YUVDECODE)
      System.out.print("YUV " + subName[subsamp] + " ... ");
    else {
      System.out.print(pfStr + " ");
      if((flags & TJ.FLAG_BOTTOMUP) != 0) System.out.print("Bottom-Up ");
      else System.out.print("Top-Down  ");
      if(!sf.isOne())
        System.out.print(sf.getNum() + "/" + sf.getDenom() + " ... ");
      else System.out.print("... ");
    }

    t = getTime();
    tjd.setJPEGImage(jpegBuf, jpegSize);
    if(tjd.getWidth() != w || tjd.getHeight() != h
      || tjd.getSubsamp() != subsamp)
      throw new Exception("Incorrect JPEG header");

    temp1 = scaledWidth;
    temp2 = scaledHeight;
    temp1 = tjd.getScaledWidth(temp1, temp2);
    temp2 = tjd.getScaledHeight(temp1, temp2);
    if(temp1 != scaledWidth || temp2 != scaledHeight)
      throw new Exception("Scaled size mismatch");

    if(yuv == YUVDECODE) dstBuf = tjd.decompressToYUV(flags);
    else {
      if(bi)
        img = tjd.decompress(scaledWidth, scaledHeight, biType[pf], flags);
      else dstBuf = tjd.decompress(scaledWidth, 0, scaledHeight, pf, flags);
    }
    t = getTime() - t;

    if(bi) {
      tempstr = baseName + "_dec_" + pfStr + "_"
        + (((flags & TJ.FLAG_BOTTOMUP) != 0) ? "BU" : "TD") + "_"
        + subName[subsamp] + "_" + (double)sf.getNum() / (double)sf.getDenom()
        + "x" + ".png";
      File file = new File(tempstr);
      ImageIO.write(img, "png", file);
    }

    if(yuv == YUVDECODE) {
      if(checkBufYUV(dstBuf, dstBuf.length, w, h, subsamp) == 1)
        System.out.print("Passed.");
      else {
        System.out.print("FAILED!");  exitStatus = -1;
      }
    }
    else {
      if((bi && checkImg(img, pf, subsamp, sf, flags) == 1)
        || (!bi && checkBuf(dstBuf, scaledWidth, scaledWidth
          * TJ.getPixelSize(pf), scaledHeight, pf, subsamp, sf, flags) == 1))
        System.out.print("Passed.");
      else {
        System.out.print("FAILED!");  exitStatus = -1;
      }
    }
    System.out.format("  %.6f ms\n", t * 1000.);
  }

  private static void decompTest(TJDecompressor tjd, byte[] jpegBuf,
    int jpegSize, int w, int h, int pf, String baseName, int subsamp,
    int flags) throws Exception {
    int i;
    if((subsamp == TJ.SAMP_444 || subsamp == TJ.SAMP_GRAY) && yuv == 0) {
      TJScalingFactor sf[] = TJ.getScalingFactors();
      for(i = 0; i < sf.length; i++)
        decompTest(tjd, jpegBuf, jpegSize, w, h, pf, baseName, subsamp,
          flags, sf[i]);
    }
    else
      decompTest(tjd, jpegBuf, jpegSize, w, h, pf, baseName, subsamp,
        flags, new TJScalingFactor(1, 1));
    System.out.print("\n");
  }

  private static void doTest(int w, int h, int[] formats, int subsamp,
    String baseName) throws Exception {
    TJCompressor tjc = null;
    TJDecompressor tjd = null;
    int size;
    byte[] dstBuf;

    if(yuv == YUVENCODE) dstBuf = new byte[TJ.bufSizeYUV(w, h, subsamp)];
    else dstBuf = new byte[TJ.bufSize(w, h)];

    try {
      tjc = new TJCompressor();
      tjd = new TJDecompressor();  

      for(int pf : formats) {
        for(int i = 0; i < 2; i++) {
          int flags = 0;
          if(i == 1) {
            if(yuv == YUVDECODE) {
              tjc.close();  tjd.close();  return;
            }
            else flags |= TJ.FLAG_BOTTOMUP;
          }
          size = compTest(tjc, dstBuf, w, h, pf, baseName, subsamp, 100,
            flags);
          decompTest(tjd, dstBuf, size, w, h, pf, baseName, subsamp, flags);
        }
      }
    }
    catch(Exception e) {
      if(tjc != null) tjc.close();
      if(tjd != null) tjd.close();
      throw e;
    }
    if(tjc != null) tjc.close();
    if(tjd != null) tjd.close();
  }

  private static void doTest1() throws Exception {
    int w, h, i;
    byte[] srcBuf, jpegBuf;
    TJCompressor tjc = null;

    try {
      tjc = new TJCompressor();
      System.out.println("Buffer size regression test");
      for(w = 1; w < 48; w++) {
        int maxh = (w == 1) ? 2048 : 48;
        for(h = 1; h < maxh; h++) {
          if(h % 100 == 0)
            System.out.format("%04d x %04d\b\b\b\b\b\b\b\b\b\b\b", w, h);
          srcBuf = new byte[w * h * 4];
          jpegBuf = new byte[TJ.bufSize(w, h)];
          Arrays.fill(srcBuf, (byte)0);
          for(i = 0; i < w * h; i++) {
            srcBuf[i * 4] = pixels[i % 9][0];
            srcBuf[i * 4 + 1] = pixels[i % 9][1];
            srcBuf[i * 4 + 2] = pixels[i % 9][2];
          }
          tjc.setSourceImage(srcBuf, w, 0, h, TJ.PF_BGRX);
          tjc.setSubsamp(TJ.SAMP_444);
          tjc.setJPEGQuality(100);
          tjc.compress(jpegBuf, 0);

          srcBuf = new byte[h * w * 4];
          jpegBuf = new byte[TJ.bufSize(h, w)];
          for(i = 0; i < h * w; i++) {
            if(i % 2 == 0) srcBuf[i * 4] =
                srcBuf[i * 4 + 1] = srcBuf[i * 4 + 2] = (byte)0xFF;
            else srcBuf[i * 4] = srcBuf[i * 4 + 1] = srcBuf[i * 4 + 2] = 0;
          }
          tjc.setSourceImage(srcBuf, h, 0, w, TJ.PF_BGRX);
          tjc.compress(jpegBuf, 0);
        }
      }
      System.out.println("Done.      ");
    }
    catch(Exception e) {
      if(tjc != null) tjc.close();
      throw e;
    }
    if(tjc != null) tjc.close();
  }

  public static void main(String argv[]) {
    try {
      String testName = "javatest";
      boolean doyuv = false;
      for(int i = 0; i < argv.length; i++) {
        if(argv[i].equalsIgnoreCase("-yuv")) doyuv = true;
        if(argv[i].substring(0, 1).equalsIgnoreCase("-h")
          || argv[i].equalsIgnoreCase("-?"))
          usage();
        if(argv[i].equalsIgnoreCase("-bi")) {
          bi = true;
          testName = "javabitest";
        }
      }
      if(doyuv) yuv = YUVENCODE;
      doTest(35, 39, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_444, testName);
      doTest(39, 41, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_444, testName);
      if(doyuv) {
        doTest(41, 35, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_422,
          testName);
        doTest(35, 39, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_422,
          testName);
        doTest(39, 41, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_420,
          testName);
        doTest(41, 35, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_420,
          testName);
        doTest(35, 39, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_440,
          testName);
        doTest(39, 41, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_440,
          testName);
      }
      doTest(35, 39, onlyGray, TJ.SAMP_GRAY, testName);
      doTest(39, 41, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_GRAY,
        testName);
      doTest(41, 35, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_GRAY,
        testName);
      if(!doyuv && !bi) doTest1();
      if(doyuv && !bi) {
        yuv = YUVDECODE;
        doTest(48, 48, onlyRGB, TJ.SAMP_444, "javatest_yuv0");
        doTest(35, 39, onlyRGB, TJ.SAMP_444, "javatest_yuv1");
        doTest(48, 48, onlyRGB, TJ.SAMP_422, "javatest_yuv0");
        doTest(39, 41, onlyRGB, TJ.SAMP_422, "javatest_yuv1");
        doTest(48, 48, onlyRGB, TJ.SAMP_420, "javatest_yuv0");
        doTest(41, 35, onlyRGB, TJ.SAMP_420, "javatest_yuv1");
        doTest(48, 48, onlyRGB, TJ.SAMP_440, "javatest_yuv0");
        doTest(35, 39, onlyRGB, TJ.SAMP_440, "javatest_yuv1");
        doTest(48, 48, onlyRGB, TJ.SAMP_GRAY, "javatest_yuv0");
        doTest(35, 39, onlyRGB, TJ.SAMP_GRAY, "javatest_yuv1");
        doTest(48, 48, onlyGray, TJ.SAMP_GRAY, "javatest_yuv0");
        doTest(39, 41, onlyGray, TJ.SAMP_GRAY, "javatest_yuv1");
      }
    }
    catch(Exception e) {
      e.printStackTrace();
      exitStatus = -1;
    }
    System.exit(exitStatus);
  }
}
