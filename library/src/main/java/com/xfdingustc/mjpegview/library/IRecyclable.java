package com.xfdingustc.mjpegview.library;

// objects that can be recycled for reuse (avoid GC)
// TODO - use ref counter

// implementation classes:
//	BitmapBuffer
//	ByteArrayBuffer

public interface IRecyclable {
	void recycle();
}
