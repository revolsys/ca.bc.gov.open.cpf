/*
 * Copyright © 2008-2016, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.plugins.mandelbrot;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.DefaultValue;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;

import com.revolsys.util.Exceptions;

@BusinessApplicationPlugin(perRequestResultData = true, numRequestsPerWorker = 1,
    resultDataContentTypes = {
      "image/png", "image/gif", "image/jpeg"
    }, instantModePermission = "denyAll", description = "Create a Mandelbrot image")
public class Mandelbrot {
  public static int mand(final Complex z0, final int max) {
    Complex z = z0;
    for (int interation = 0; interation < max; interation++) {
      if (z.abs() > 2.0) {
        return interation;
      }
      z = z.times(z).plus(z0);
    }
    return max - 1;
  }

  private int imageWidth = 256;

  private double centreX = -0.5;

  private double centreY = 0;

  private double width = 3;

  private double height = 2;

  private int imageHeight = 256;

  private String resultDataContentType;

  private OutputStream resultData;

  private int maxIterations = 255;

  private boolean useColours = true;

  public void execute() {
    final BufferedImage image = new BufferedImage(this.imageWidth, this.imageHeight,
      BufferedImage.TYPE_INT_RGB);
    Color[] colors = null;
    if (this.useColours) {
      colors = new Color[this.maxIterations];
      for (int i = 0; i < this.maxIterations; i++) {
        colors[i] = Color.getHSBColor(i / 256f, 1, i / (i + 8f));
      }
    }
    for (int i = 0; i < this.imageWidth; i++) {
      for (int j = 0; j < this.imageHeight; j++) {
        final double x0 = this.centreX - this.width / 2 + this.width * i / this.imageWidth;
        final double y0 = this.centreY - this.height / 2 + this.height * j / this.imageHeight;
        final Complex z0 = new Complex(x0, y0);
        final int iteration = mand(z0, this.maxIterations);
        Color color;
        if (this.useColours) {
          color = colors[iteration];
        } else {
          final float gray = (float)(this.maxIterations - iteration) / this.maxIterations;
          color = new Color(gray, gray, gray);
        }
        image.setRGB(i, this.imageHeight - 1 - j, color.getRGB());
      }
    }
    final ImageWriter imageWriter = ImageIO.getImageWritersByMIMEType(this.resultDataContentType)
      .next();
    try {
      imageWriter.setOutput(ImageIO.createImageOutputStream(this.resultData));
      imageWriter.write(image);
    } catch (final IOException e) {
      throw Exceptions.wrap("Unable to create image file", e);
    } finally {
      imageWriter.dispose();
    }
  }

  @RequestParameter(index = 5)
  @DefaultValue("-0.5")
  public void setCentreX(final double centreX) {
    this.centreX = centreX;
  }

  @RequestParameter(index = 6)
  @DefaultValue("0")
  public void setCentreY(final double centreY) {
    this.centreY = centreY;
  }

  @RequestParameter(index = 8)
  @DefaultValue("2")
  public void setHeight(final double height) {
    this.height = height;
  }

  @JobParameter(index = 2)
  @RequestParameter
  @DefaultValue("200")
  public void setImageHeight(final int imageHeight) {
    this.imageHeight = imageHeight;
  }

  @JobParameter(index = 1)
  @RequestParameter
  @DefaultValue("300")
  public void setImageWidth(final int imageWidth) {
    this.imageWidth = imageWidth;
  }

  @JobParameter(index = 3)
  @RequestParameter
  @DefaultValue("255")
  public void setMaxIterations(final int maxIterations) {
    this.maxIterations = maxIterations;
  }

  public void setResultData(final OutputStream resultData) {
    this.resultData = resultData;
  }

  public void setResultDataContentType(final String resultDataContentType) {
    this.resultDataContentType = resultDataContentType;
  }

  @JobParameter(index = 4)
  @RequestParameter
  @DefaultValue("true")
  public void setUseColours(final boolean useColours) {
    this.useColours = useColours;
  }

  @RequestParameter(index = 7)
  @DefaultValue("3")
  public void setWidth(final double width) {
    this.width = width;
  }
}
