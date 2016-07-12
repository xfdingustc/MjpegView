package com.xfdingustc.mjpegview.library;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class MjpegBuffer {

	static final boolean DEBUG = false;
	static final String TAG = "MjpegBuffer";

	public static final int MAX_LENGTH = 512;

	private final byte[] data = new byte[MAX_LENGTH];
	private final InputStream input;

	private int offset; // valid data start
	private int size; // valid data size

	private static final byte[] HTTP_END = { '\r', '\n', '\r', '\n' };
	// Content-Length:
	private static final byte[] Content_Length = { 'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'L', 'e', 'n', 'g', 't',
			'h', ':' };

	public MjpegBuffer(InputStream input) {
		this.input = input;
	}

	@Override
	public String toString() {
		return new String(data, offset, size, Charset.forName("UTF-8"));
	}



	private void readFully(byte[] buf, int offset, int size) throws IOException {
		while (size > 0) {
			int n = input.read(buf, offset, size);
			if (n < 0) {
				throw new IOException("readFully returns " + n);
			}
			offset += n;
			size -= n;
		}
	}

	public void refill() throws IOException {
		if (DEBUG) {
			Log.d(TAG, "refill");
		}
		if (size > 0) {
			System.arraycopy(data, offset, data, 0, size);
		}
		offset = 0;
		int toread = data.length - size;
		if (toread > 0) {
			readFully(data, size, toread);
			size += toread;
		}
	}

	public void read(byte[] buf, int offset, int size) throws IOException {
		if (DEBUG) {
			Log.d(TAG, "read, offset=" + offset + ", size=" + size);
		}
		if (this.size > 0) {
			int tocopy = this.size;
			if (tocopy > size) {
				tocopy = size;
			}
			System.arraycopy(this.data, this.offset, buf, 0, tocopy);
			this.offset += tocopy;
			this.size -= tocopy;
			offset += tocopy;
			size -= tocopy;
		}
		if (size > 0) {
			readFully(buf, offset, size);
		}
	}

	private int findSequence(final byte[] seq) {
		int seqlen = seq.length;
		int len = size - seqlen;
		for (int index = offset, i = len; i > 0; i--, index++) {
			if (data[index] == seq[0]) {
				int src_index = index + 1;
				int dst_index = 1;
				int cmp = seqlen - 1;
				for (; cmp > 0; cmp--) {
					if (data[src_index] != seq[dst_index]) {
						break;
					}
					src_index++;
					dst_index++;
				}
				if (cmp == 0) {
					return index;
				}
			}
		}
		return -1;
	}

	private void skip(int offset, final byte[] seq) {
		int delta = offset - this.offset + seq.length;
		this.offset += delta;
		this.size -= delta;
	}

	public void skipHttpEnd() throws IOException {
		int offset = findSequence(HTTP_END);
		if (offset < 0) {
			throw new IOException("http response end not found");
		}
		if (DEBUG) {
			Log.d(TAG, "skipHttpEnd, offset at " + offset);
		}
		skip(offset, HTTP_END);
	}

	public void skipContentLength() throws IOException {
		int offset = findSequence(Content_Length);
		if (offset < 0) {
			throw new IOException("Content-Length not found");
		}
		if (DEBUG) {
			Log.d(TAG, "skipContentLength, offset at " + offset);
		}
		skip(offset, Content_Length);
	}

	public int scanInteger() throws IOException {
		if (size == 0) {
			throw new IOException("scanInteger: no more bytes");
		}

		while (data[offset] == ' ') {
			offset++;
			size--;
			if (size == 0) {
				throw new IOException("scanInteger: no more bytes");
			}
		}

		int result = 0;
		int index = offset;
		for (int n = size; n > 0; n--, index++) {
			byte v = data[index];
			if (v >= '0' && v <= '9') {
				result = result * 10 + (v - '0');
			} else {
				break;
			}
		}
		size -= (index - offset);
		offset = index;

		if (DEBUG) {
			Log.d(TAG, "scanInteger returns " + result);
		}

		return result;
	}
}
