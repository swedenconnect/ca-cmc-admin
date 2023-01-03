/*
 * Copyright (c) 2022-2023.  Agency for Digital Government (DIGG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.swedenconnect.ca.cmcclient.configuration;

import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;

/**
 * Holding a service logotype element
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Getter
public class EmbeddedLogo {

  /** Media type value for JPG images */
  public static final String JPG = MediaType.IMAGE_JPEG_VALUE;
  /** Media type value for PNG images */
  public static final String PNG = MediaType.IMAGE_PNG_VALUE;
  /** Media type value for BMP images */
  public static final String BMP = "image/bmp";
  /** Media type value for GIF images */
  public static final String GIF = MediaType.IMAGE_GIF_VALUE;
  /** Media type value for SVG images */
  public static final String SVG = "image/svg+xml";
  /** Logotype image bytes */
  private final byte[] logoData;
  /** Base64Encoded image bytes */
  private final String b64Logo;
  /** Holds the data URL of the image that fits into the src attribute of a html img element */
  private final String imgSrc;
  /** the mime type fo the logotype image */
  private final String logoMimeType;

  /**
   * Contructor of the EmbeddedLogo object
   * @param location the logo location
   * @param resourceLoader resource loader
   * @throws Exception error parsing data
   */
  public EmbeddedLogo(String location, ResourceLoader resourceLoader) throws Exception {
    Resource logoResource = getResource(location, resourceLoader);
    this.logoData = IOUtils.toByteArray(logoResource.getInputStream());
    this.b64Logo = Base64.toBase64String(this.logoData);
    this.logoMimeType = getMimeType(logoResource.getFilename());
    this.imgSrc = getImageSource();
  }

  private Resource getResource(String location, ResourceLoader resourceLoader) {
    if (location.toLowerCase().startsWith("classpath:")){
      return resourceLoader.getResource(location);
    }
    return new FileSystemResource(location);
  }

  /**
   * Produces the appropriate data url "src" attribute value of an img html tag
   * @return Data URL value for the image.
   */
  private String getImageSource() {
    // src="data:image/png;base64,iVBOR...rkJggg=="
    return "data:" + logoMimeType + ";base64," + b64Logo;
  }


  /**
   * Method for providing the mime type of an image file based on file extension.
   * @param source The file name or path of an image file with an appropriate file extension.
   * @return The image file media type specified by file extension.
   */
  public static String getMimeType(String source) {
    String type = source.substring(source.lastIndexOf(".") + 1);

    switch (type) {
    case "jpg":
    case "jpeg":
      return JPG;
    case "gif":
      return GIF;
    case "bmp":
      return BMP;
    case "svg":
      return SVG;
    default:
      return PNG;
    }
  }

}
