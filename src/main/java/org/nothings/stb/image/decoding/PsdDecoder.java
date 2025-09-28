package org.nothings.stb.image.decoding;

import org.nothings.stb.image.ColorComponents;
import org.nothings.stb.image.ImageInfo;
import org.nothings.stb.image.ImageResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PsdDecoder extends Decoder {
	private PsdDecoder(InputStream stream) {
		super(stream);
	}

	private int stbi__psd_decode_rle(FakePtrByte po, int pixelCount) throws IOException {
		FakePtrByte p = po.clone();
		int count = 0;
		int nleft = 0;
		int len = 0;
		count = 0;
		while ((nleft = pixelCount - count) > 0) {
			len = stbi__get8();
			if (len == 128) {
			} else if (len < 128) {
				len++;
				if (len > nleft)
					return 0;
				count += len;
				while (len != 0) {
					p.set(stbi__get8());
					p.move(4);
					len--;
				}
			} else if (len > 128) {
				short val = 0;
				len = 257 - len;
				if (len > nleft)
					return 0;
				val = stbi__get8();
				count += len;
				while (len != 0) {
					p.set(val);
					p.move(4);
					len--;
				}
			}
		}

		return 1;
	}

    private int stbi__psd_decode_rle16(FakePtrByte po, int pixelCount) throws IOException {
        FakePtrByte p = po.clone();
        int count = 0;
        while (pixelCount - count > 0) {
            int len = stbi__get8();
            if (len == 128) {
                // no-op
            } else if (len < 128) {
                len++; // literal run of 'len' samples
                if (len > pixelCount - count) return 0;
                count += len;
                while (len-- != 0) {
                    int v = stbi__get16be();            // one 16-bit sample
                    p.set((short)((v >> 8) & 0xFF));     // high byte
                    p.setAt(1, (short)(v & 0xFF));       // low byte
                    p.move(8);                           // advance to next pixel, same channel (RGBA16 -> 8 bytes/pixel)
                }
            } else { // len > 128
                int run = 257 - len;                    // repeat run
                if (run > pixelCount - count) return 0;
                int v = stbi__get16be();
                count += run;
                while (run-- != 0) {
                    p.set((short)((v >> 8) & 0xFF));
                    p.setAt(1, (short)(v & 0xFF));
                    p.move(8);
                }
            }
        }
        return 1;
    }

    private ImageResult InternalDecode(ColorComponents requiredComponents, int bpc) throws IOException {
        int pixelCount, channelCount, compression, channel, i, bitdepth, w, h;
        byte[] _out_;

        if (stbi__get32be() != 0x38425053) stbi__err("not PSD");
        if (stbi__get16be() != 1)          stbi__err("wrong version");
        stbi__skip(6);
        channelCount = stbi__get16be();
        if (channelCount < 0 || channelCount > 16) stbi__err("wrong channel count");
        h = (int) stbi__get32be();
        w = (int) stbi__get32be();
        bitdepth = stbi__get16be();
        if (bitdepth != 8 && bitdepth != 16) stbi__err("unsupported bit depth");
        if (stbi__get16be() != 3)            stbi__err("wrong color format"); // RGB
        stbi__skip((int) stbi__get32be());   // color mode data
        stbi__skip((int) stbi__get32be());   // image resources
        stbi__skip((int) stbi__get32be());   // layer/mask
        compression = stbi__get16be();       // 0 = raw, 1 = RLE
        if (compression > 1) stbi__err("bad compression");

        // Decide output bit depth: if caller asks for 16 (bpc==16) and PSD has 16, output 16; else 8-bit.
        int bits_per_channel = (bitdepth == 16 && bpc == 16) ? 16 : 8;
        _out_ = new byte[(bits_per_channel == 16 ? 8 : 4) * w * h];
        pixelCount = w * h;

        FakePtrByte ptr = new FakePtrByte(_out_);

        if (compression != 0) {
            // Skip per-row byte counts: h * channelCount * 2 bytes
            stbi__skip(h * channelCount * 2);

            for (channel = 0; channel < 4; channel++) {
                FakePtrByte p = new FakePtrByte(ptr, channel); // channel offset 0..3
                if (channel >= channelCount) {
                    // Fill missing channels
                    if (bits_per_channel == 16) {
                        // default: A=0xFFFF, others=0x0000
                        int hi = (channel == 3) ? 0xFF : 0x00;
                        int lo = (channel == 3) ? 0xFF : 0x00;
                        for (i = 0; i < pixelCount; i++, p.move(8)) {
                            p.set((short)hi);
                            p.setAt(1, (short)lo);
                        }
                    } else {
                        short val = (short)(channel == 3 ? 255 : 0);
                        for (i = 0; i < pixelCount; i++, p.move(4)) p.set(val);
                    }
                } else {
                    // Decode one planar channel into interleaved RGBA
                    if (bits_per_channel == 16) {
                        if (stbi__psd_decode_rle16(p, pixelCount) == 0) stbi__err("corrupt");
                    } else {
                        if (stbi__psd_decode_rle(p, pixelCount) == 0) stbi__err("corrupt");
                    }
                }
            }
        } else {
            // Raw (uncompressed) planar data
            for (channel = 0; channel < 4; channel++) {
                FakePtrByte p = new FakePtrByte(ptr, channel);
                if (channel >= channelCount) {
                    if (bits_per_channel == 16) {
                        int hi = (channel == 3) ? 0xFF : 0x00;
                        int lo = (channel == 3) ? 0xFF : 0x00;
                        for (i = 0; i < pixelCount; i++, p.move(8)) {
                            p.set((short)hi);
                            p.setAt(1, (short)lo);
                        }
                    } else {
                        short val = (short)(channel == 3 ? 255 : 0);
                        for (i = 0; i < pixelCount; i++, p.move(4)) p.set(val);
                    }
                } else {
                    if (bits_per_channel == 16) {
                        // Read 16-bit sample, store big-endian, advance 8
                        for (i = 0; i < pixelCount; i++, p.move(8)) {
                            int v = stbi__get16be();
                            p.set((short)((v >> 8) & 0xFF));
                            p.setAt(1, (short)(v & 0xFF));
                        }
                    } else {
                        // We output 8-bit; if PSD is 16-bit, drop low byte (>>8), else just read a byte
                        if (bitdepth == 16) {
                            for (i = 0; i < pixelCount; i++, p.move(4))
                                p.set((short)(stbi__get16be() >> 8));
                        } else {
                            for (i = 0; i < pixelCount; i++, p.move(4))
                                p.set(stbi__get8());
                        }
                    }
                }
            }
        }

        // Un-premultiply straight alpha if PSD stored premultiplied data (match your 8-bit logic)
        if (channelCount >= 4) {
            if (bits_per_channel == 16) {
                for (int px = 0; px < w * h; ++px) {
                    int off = px * 8;
                    int r = (( _out_[off    ] & 0xFF) << 8) | (_out_[off + 1] & 0xFF);
                    int g = (( _out_[off + 2] & 0xFF) << 8) | (_out_[off + 3] & 0xFF);
                    int b = (( _out_[off + 4] & 0xFF) << 8) | (_out_[off + 5] & 0xFF);
                    int a = (( _out_[off + 6] & 0xFF) << 8) | (_out_[off + 7] & 0xFF);
                    if (a != 0 && a != 65535) {
                        float af = a / 65535.0f;
                        float ra = 1.0f / af;
                        float inv_a = 65535.0f * (1 - ra);
                        int nr = (int)(r * ra + inv_a);
                        int ng = (int)(g * ra + inv_a);
                        int nb = (int)(b * ra + inv_a);
                        if (nr < 0) nr = 0; else if (nr > 65535) nr = 65535;
                        if (ng < 0) ng = 0; else if (ng > 65535) ng = 65535;
                        if (nb < 0) nb = 0; else if (nb > 65535) nb = 65535;
                        _out_[off    ] = (byte)((nr >> 8) & 0xFF); _out_[off + 1] = (byte)(nr & 0xFF);
                        _out_[off + 2] = (byte)((ng >> 8) & 0xFF); _out_[off + 3] = (byte)(ng & 0xFF);
                        _out_[off + 4] = (byte)((nb >> 8) & 0xFF); _out_[off + 5] = (byte)(nb & 0xFF);
                    }
                }
            } else {
                for (i = 0; i < w * h; ++i) {
                    int off = 4 * i;
                    int a = _out_[off + 3] & 0xFF;
                    if (a != 0 && a != 255) {
                        float af = a / 255.0f;
                        float ra = 1.0f / af;
                        float inv_a = 255.0f * (1 - ra);
                        _out_[off    ] = (byte) (( (_out_[off    ] & 0xFF) * ra + inv_a));
                        _out_[off + 1] = (byte) (( (_out_[off + 1] & 0xFF) * ra + inv_a));
                        _out_[off + 2] = (byte) (( (_out_[off + 2] & 0xFF) * ra + inv_a));
                    }
                }
            }
        }

        int req_comp = ColorComponents.toReqComp(requiredComponents);
        if (req_comp != 0 && req_comp != 4) {
            if (bits_per_channel == 16)
                _out_ = Utility.stbi__convert_format16(_out_, 4, req_comp, w, h);
            else
                _out_ = Utility.stbi__convert_format(_out_, 4, req_comp, w, h);
        }

        return new ImageResult(
                w, h,
                ColorComponents.RedGreenBlueAlpha,
                requiredComponents != null ? requiredComponents : ColorComponents.RedGreenBlueAlpha,
                bits_per_channel,
                _out_
        );
    }

    public static boolean Test(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			return Utility.stbi__get32be(stream) == 0x38425053;
		} catch (Exception ex) {
			return false;
		}
	}

	public static ImageInfo Info(byte[] data) {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(data);
			if (Utility.stbi__get32be(stream) != 0x38425053) return null;

			if (Utility.stbi__get16be(stream) != 1) return null;

			Utility.stbi__skip(stream, 6);
			int channelCount = Utility.stbi__get16be(stream);
			if (channelCount < 0 || channelCount > 16) return null;

			int height = (int) Utility.stbi__get32be(stream);
			int width = (int) Utility.stbi__get32be(stream);
			int depth = Utility.stbi__get16be(stream);
			if (depth != 8 && depth != 16) return null;

			if (Utility.stbi__get16be(stream) != 3) return null;

			return new ImageInfo(width, height, ColorComponents.RedGreenBlueAlpha, depth);
		} catch (Exception ex) {
			return null;
		}
	}

	public static ImageResult Decode(byte[] data, ColorComponents requiredComponents, int bpc) throws IOException {
		ByteArrayInputStream stream = new ByteArrayInputStream(data);
		PsdDecoder decoder = new PsdDecoder(stream);
		return decoder.InternalDecode(requiredComponents, bpc);
	}

	public static ImageResult Decode(byte[] data, ColorComponents requiredComponents) throws IOException {
		return Decode(data, requiredComponents, 8);
	}

	public static ImageResult Decode(byte[] data) throws IOException {
		return Decode(data, null);
	}
}