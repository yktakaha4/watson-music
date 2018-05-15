package com.github.yktakaha4.watsonmusic.util;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.math3.util.FastMath;
import org.springframework.stereotype.Component;

@Component
public class ImageUtils {
  private final static float MAX_SIZE = 300;
  private final static float QUALITY = 0.5f;

  public byte[] optimizeToJpeg(byte[] src) throws IOException {
    try (ByteArrayInputStream is = new ByteArrayInputStream(src);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
      BufferedImage srcImage = ImageIO.read(is);
      double scale = new Integer(FastMath.max(srcImage.getWidth(), srcImage.getHeight())).doubleValue() / MAX_SIZE;

      BufferedImage destImage = resizeImage(srcImage, scale);
      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(QUALITY);
      writer.setOutput(ios);
      writer.write(null, new IIOImage(destImage, null, null), param);
      writer.dispose();

      return os.toByteArray();
    }
  }

  private BufferedImage resizeImage(BufferedImage image, double scale) throws IOException {
    int width = (int) (image.getWidth() * scale);
    int height = (int) (image.getHeight() * scale);
    BufferedImage resizedImage = new BufferedImage(width, height, image.getType());
    AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
    AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
    op.filter(image, resizedImage);

    return resizedImage;
  }

}
