# StbImageJava

StbImageJava is Java port of stb_image.h 2.22. Or - in other words - it's a Java library that can load images in JPG, PNG, BMP, TGA, PSD and GIF formats.

# Maven
```html
<repository>
  <id>loohp-repo</id>
  <url>https://repo.loohpjames.com/repository</url>
</repository>
```
```html
<dependency>
  <groupId>org.nothings.stb</groupId>
  <artifactId>StbImageJava</artifactId>
  <version>VERSION</version>
  <scope>compile</scope>
</dependency>
```
Replace `VERSION` with the version number.

# Usage
The following code loads an image from byte array and converts it to 32-bit RGBA:
```java
byte[] bytes = Files.readAllBytes(new File("image.jpg").toPath());
ImageResult image = ImageResult.FromData(bytes, ColorComponents.RedGreenBlueAlpha, true);
```
The following code converts the result into a `java.awt.image.BufferedImage`
```java
int w = image.getWidth();
int h = image.getHeight();
BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
byte[] colors = image.getData();
for (int i = 0; i < w * h; i++) {
    int index = i * 4;
    int r = (colors[index] << 16) & 0x00FF0000;
    int g = (colors[index + 1] << 8) & 0x0000FF00;
    int b = colors[index + 2] & 0x000000FF;
    int a = (colors[index + 3] << 24) & 0xFF000000;
    bufferedImage.setRGB(i % w, i / w, a | r | g | b);
}
```

# License
Public Domain (In honour of upstream)

# Credits
* [StbImageJava](https://github.com/StbJava/StbImageJava) (Upstream)
* [stb](https://github.com/nothings/stb)
