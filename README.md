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
Following code loads image from byte array and converts it to 32-bit RGBA:
```java
  byte[] bytes = Files.readAllBytes(new File("image.jpg").toPath());
  ImageResult image = ImageResult.FromData(bytes, ColorComponents.RedGreenBlueAlpha, true);
```

# License
Public Domain (In honour of upstream)

# Credits
* [StbImageJava](https://github.com/StbJava/StbImageJava) (Upstream)
* [stb](https://github.com/nothings/stb)
