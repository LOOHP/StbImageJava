package org.nothings.stb.image.decoding;

import java.io.IOException;
import java.io.InputStream;

public class Utility
{
	public static int stbi__bitreverse16(int n) {
		n = ((n & 0xAAAA) >> 1) | ((n & 0x5555) << 1);
		n = ((n & 0xCCCC) >> 2) | ((n & 0x3333) << 2);
		n = ((n & 0xF0F0) >> 4) | ((n & 0x0F0F) << 4);
		n = ((n & 0xFF00) >> 8) | ((n & 0x00FF) << 8);
		return n;
	}

	public static int stbi__bit_reverse(int v, int bits) {
		return stbi__bitreverse16(v) >> (16 - bits);
	}

	public static long _lrotl(long x, int y) {
		return (x << y) | (x >> (32 - y));
	}

	protected static void stbi__skip(InputStream s, int count) throws IOException {
		s.skip(count);
	}

	public static short stbi__get8(InputStream s) throws IOException
	{
		int b = s.read();
		if (b == -1)
		{
			throw new IOException("EOF");
		}

		return (short)b;
	}

	public static int stbi__get16be(InputStream s) throws IOException
	{
		int z = stbi__get8(s);
		return (z << 8) + stbi__get8(s);
	}

	public static long stbi__get32be(InputStream s) throws IOException
	{
		long z = stbi__get16be(s);
		return (z << 16) + stbi__get16be(s);
	}

	public static int stbi__get16le(InputStream s) throws IOException
	{
		int z = stbi__get8(s);
		return z + (stbi__get8(s) << 8);
	}

	public static long stbi__get32le(InputStream s) throws IOException
	{
		long z = stbi__get16le(s);
		return z + (stbi__get16le(s) << 16);
	}

	public static short stbi__compute_y(int r, int g, int b)
	{
		return (short)(((r * 77) + (g * 150) + (29 * b)) >> 8);
	}

	public static int stbi__compute_y_16(int r, int g, int b)
	{
		return ((r * 77) + (g * 150) + (29 * b)) >> 8;
	}

	public static byte[] stbi__convert_format16(byte[] data, int img_n, int req_comp, long x, long y) throws IOException {
        int xi = (int)x, yi = (int)y;
        if (req_comp == img_n) return data;

        int inStep  = img_n * 2;
        int outStep = req_comp * 2;
        byte[] out = new byte[yi * xi * outStep];

        for (int j = 0; j < yi; ++j) {
            int srcRow = j * xi * inStep;
            int dstRow = j * xi * outStep;

            switch (img_n * 8 + req_comp) {
                // 1 -> 2 (G -> GA)
                case (1 * 8 + 2): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        // gray
                        out[d]   = data[s];
                        out[d+1] = data[s+1];
                        // alpha = 0xFFFF
                        out[d+2] = (byte)0xFF; out[d+3] = (byte)0xFF;
                    }
                    break;
                }
                // 1 -> 3 (G -> RGB)
                case (1 * 8 + 3): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        byte gh = data[s], gl = data[s+1];
                        out[d]   = gh; out[d+1] = gl; // R
                        out[d+2] = gh; out[d+3] = gl; // G
                        out[d+4] = gh; out[d+5] = gl; // B
                    }
                    break;
                }
                // 1 -> 4 (G -> RGBA)
                case (1 * 8 + 4): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        byte gh = data[s], gl = data[s+1];
                        out[d]   = gh; out[d+1] = gl; // R
                        out[d+2] = gh; out[d+3] = gl; // G
                        out[d+4] = gh; out[d+5] = gl; // B
                        out[d+6] = (byte)0xFF; out[d+7] = (byte)0xFF; // A
                    }
                    break;
                }
                // 2 -> 1 (GA -> G)
                case (2 * 8 + 1): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        out[d]   = data[s];
                        out[d+1] = data[s+1];
                    }
                    break;
                }
                // 2 -> 3 (GA -> RGB)
                case (2 * 8 + 3): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        byte gh = data[s], gl = data[s+1];
                        out[d]   = gh; out[d+1] = gl;
                        out[d+2] = gh; out[d+3] = gl;
                        out[d+4] = gh; out[d+5] = gl;
                    }
                    break;
                }
                // 2 -> 4 (GA -> RGBA)
                case (2 * 8 + 4): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        byte gh = data[s], gl = data[s+1];
                        out[d]   = gh; out[d+1] = gl;
                        out[d+2] = gh; out[d+3] = gl;
                        out[d+4] = gh; out[d+5] = gl;
                        out[d+6] = data[s+2]; out[d+7] = data[s+3]; // preserve alpha
                    }
                    break;
                }
                // 3 -> 4 (RGB -> RGBA)
                case (3 * 8 + 4): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        out[d]   = data[s];   out[d+1] = data[s+1];
                        out[d+2] = data[s+2]; out[d+3] = data[s+3];
                        out[d+4] = data[s+4]; out[d+5] = data[s+5];
                        out[d+6] = (byte)0xFF; out[d+7] = (byte)0xFF;
                    }
                    break;
                }
                // 3 -> 1 (RGB -> G)
                case (3 * 8 + 1): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        int r = ((data[s]   & 0xFF) << 8) | (data[s+1] & 0xFF);
                        int g = ((data[s+2] & 0xFF) << 8) | (data[s+3] & 0xFF);
                        int b = ((data[s+4] & 0xFF) << 8) | (data[s+5] & 0xFF);
                        int y16 = stbi__compute_y_16(r, g, b) & 0xFFFF;
                        out[d]   = (byte)((y16 >> 8) & 0xFF);
                        out[d+1] = (byte)(y16 & 0xFF);
                    }
                    break;
                }
                // 3 -> 2 (RGB -> GA)
                case (3 * 8 + 2): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        int r = ((data[s]   & 0xFF) << 8) | (data[s+1] & 0xFF);
                        int g = ((data[s+2] & 0xFF) << 8) | (data[s+3] & 0xFF);
                        int b = ((data[s+4] & 0xFF) << 8) | (data[s+5] & 0xFF);
                        int y16 = stbi__compute_y_16(r, g, b) & 0xFFFF;
                        out[d]   = (byte)((y16 >> 8) & 0xFF);
                        out[d+1] = (byte)(y16 & 0xFF);
                        out[d+2] = (byte)0xFF; out[d+3] = (byte)0xFF; // A = 0xFFFF
                    }
                    break;
                }
                // 4 -> 1 (RGBA -> G)
                case (4 * 8 + 1): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        int r = ((data[s]   & 0xFF) << 8) | (data[s+1] & 0xFF);
                        int g = ((data[s+2] & 0xFF) << 8) | (data[s+3] & 0xFF);
                        int b = ((data[s+4] & 0xFF) << 8) | (data[s+5] & 0xFF);
                        int y16 = stbi__compute_y_16(r, g, b) & 0xFFFF;
                        out[d]   = (byte)((y16 >> 8) & 0xFF);
                        out[d+1] = (byte)(y16 & 0xFF);
                    }
                    break;
                }
                // 4 -> 2 (RGBA -> GA)
                case (4 * 8 + 2): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        int r = ((data[s]   & 0xFF) << 8) | (data[s+1] & 0xFF);
                        int g = ((data[s+2] & 0xFF) << 8) | (data[s+3] & 0xFF);
                        int b = ((data[s+4] & 0xFF) << 8) | (data[s+5] & 0xFF);
                        int y16 = stbi__compute_y_16(r, g, b) & 0xFFFF;
                        out[d]   = (byte)((y16 >> 8) & 0xFF);
                        out[d+1] = (byte)(y16 & 0xFF);
                        out[d+2] = data[s+6]; out[d+3] = data[s+7]; // keep alpha
                    }
                    break;
                }
                // 4 -> 3 (RGBA -> RGB)
                case (4 * 8 + 3): {
                    for (int i = xi - 1; i >= 0; --i) {
                        int s = srcRow + i * inStep;
                        int d = dstRow + i * outStep;
                        out[d]   = data[s];   out[d+1] = data[s+1];
                        out[d+2] = data[s+2]; out[d+3] = data[s+3];
                        out[d+4] = data[s+4]; out[d+5] = data[s+5];
                    }
                    break;
                }
                default:
                    Decoder.stbi__err("0");
                    break;
            }
        }

        return out;
	}

	public static byte[] stbi__convert_format(byte[] data, int img_n, int req_comp, int x, int y) throws IOException
	{
		int i = 0;
		int j = 0;
		if ((req_comp) == (img_n))
			return data;

		byte[] good = new byte[req_comp * x * y];
		for (j = 0; (j) < y; ++j)
		{
			FakePtrByte src = new FakePtrByte(data, j * x * img_n);
			FakePtrByte dest = new FakePtrByte(good, j * x * req_comp);
			switch (((img_n) * 8 + (req_comp)))
			{
				case ((1) * 8 + (2)):
					for (i = x - 1; (i) >= (0); --i, src.increase(), dest.move(2))
					{
						dest.setAt(0, src.getAt(0));
						dest.setAt(1, (short) 255);
					}
					break;
				case ((1) * 8 + (3)):
					for (i = x - 1; (i) >= (0); --i, src.increase(), dest.move(3))
					{
						int val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
					}
					break;
				case ((1) * 8 + (4)):
					for (i = x - 1; (i) >= (0); --i, src.increase(), dest.move(4))
					{
						int val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
						dest.setAt(3, (short)255);
					}
					break;
				case ((2) * 8 + (1)):
					for (i = x - 1; (i) >= (0); --i, src.move(2), dest.move(1))
					{
						int val = src.getAt(0);
						dest.setAt(0, val);
					}
					break;
				case ((2) * 8 + (3)):
					for (i = x - 1; (i) >= (0); --i, src.move(2), dest.move(3))
					{
						int val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
					}
					break;
				case ((2) * 8 + (4)):
					for (i = x - 1; (i) >= (0); --i, src.move(2), dest.move(4))
					{
						int val = src.getAt(0);
						dest.setAt(0, val);
						dest.setAt(1, val);
						dest.setAt(2, val);
						dest.setAt(3, src.getAt(1));
					}
					break;
				case ((3) * 8 + (4)):
					for (i = x - 1; (i) >= (0); --i, src.move(3), dest.move(4))
					{
						dest.setAt(0, src.getAt(0));
						dest.setAt(1, src.getAt(1));
						dest.setAt(2, src.getAt(2));
						dest.setAt(3, (short)255);
					}
					break;
				case ((3) * 8 + (1)):
					for (i = x - 1; (i) >= (0); --i, src.move(3), dest.move(1))
					{
						dest.setAt(0, stbi__compute_y(src.getAt(0), src.getAt(1), src.getAt(2)));
					}
					break;
				case ((3) * 8 + (2)):
					for (i = x - 1; (i) >= (0); --i, src.move(3), dest.move(2))
					{
						dest.setAt(0, stbi__compute_y(src.getAt(0), src.getAt(1), src.getAt(2)));
						dest.setAt(1, (short)(255));
					}
					break;
				case ((4) * 8 + (1)):
					for (i = x - 1; (i) >= (0); --i, src.move(4), dest.move(1))
					{
						dest.setAt(0, stbi__compute_y(src.getAt(0), src.getAt(1), src.getAt(2)));
					}
					break;
				case ((4) * 8 + (2)):
					for (i = x - 1; (i) >= (0); --i, src.move(4), dest.move(2))
					{
						dest.setAt(0, stbi__compute_y(src.getAt(0), src.getAt(1), src.getAt(2)));
						dest.setAt(1, src.getAt(3));
					}
					break;
				case ((4) * 8 + (3)):
					for (i = x - 1; (i) >= (0); --i, src.move(4), dest.move(3))
					{
						dest.setAt(0, src.getAt(0));
						dest.setAt(1, src.getAt(1));
						dest.setAt(2, src.getAt(2));
					}
					break;
				default:
					Decoder.stbi__err("0");
					break;
			}
		}

		return good;
	}

    public static byte[] stbi__convert_16_to_8(byte[] data16be)
    {
        // PNG stores 16-bit samples big-endian. We keep the high byte.
        int nSamples = data16be.length / 2;
        byte[] out = new byte[nSamples];
        for (int i = 0, s = 0; i < nSamples; ++i, s += 2) {
            out[i] = data16be[s]; // high byte == (value >> 8)
        }
        return out;
    }

	public static byte[] stbi__convert_16_to_8(byte[] orig, int w, int h, int channels)
	{
        int expected = w * h * channels * 2;
        if (orig.length != expected) {
            // fall back to simple version; or throw if you prefer strictness
            return stbi__convert_16_to_8(orig);
        }
        byte[] out = new byte[w * h * channels];
        for (int i = 0, s = 0; i < out.length; ++i, s += 2) {
            out[i] = orig[s];
        }
        return out;
	}

}
