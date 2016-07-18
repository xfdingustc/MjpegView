package com.xfdingustc.mjpegview.library;

import android.graphics.Bitmap;

public class BitmapBuffer implements IRecyclable {

	protected Manager mManager;
	protected Bitmap mBitmap;

	protected BitmapBuffer(Manager manager, Bitmap bitmap) {
		mManager = manager;
		mBitmap = bitmap;
	}


	public final Bitmap getBitmap() {
		return mBitmap;
	}


	public final void setBitmap(Bitmap bitmap) {
		mBitmap = bitmap;
	}

	@Override
	public void recycle() {
		mManager.recycleObject(this);
	}

	static public class Manager {

		private final BitmapBuffer[] mArray;
		private final int mMax;

		public Manager(int max) {
			mArray = new BitmapBuffer[max];
			mMax = max;
		}

		public BitmapBuffer allocateBitmap() {
			synchronized (this) {
				int i, n = mMax;
				for (i = 0; i < n; i++) {
					BitmapBuffer buffer = mArray[i];
					if (buffer != null) {
						mArray[i] = null;
						return buffer;
					}
				}
			}
			BitmapBuffer buffer = new BitmapBuffer(this, null);
			return buffer;
		}

		protected void recycleObject(BitmapBuffer bb) {
			synchronized (this) {
				int i, n = mMax;
				for (i = 0; i < n; i++) {
					if (mArray[i] == null) {
						mArray[i] = bb;
						return;
					}
				}
				mArray[0] = bb;
			}
		}

		public void clear() {
			synchronized (this) {
				int i, n = mMax;
				for (i = 0; i < n; i++) {
					BitmapBuffer bb = mArray[i];
					if (bb != null) {
						if (bb.mBitmap != null) {
							bb.mBitmap.recycle();
						}
						mArray[i] = null;
					}
				}
			}
		}

	}
}
