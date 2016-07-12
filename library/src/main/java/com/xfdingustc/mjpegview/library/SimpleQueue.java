package com.xfdingustc.mjpegview.library;

import android.os.SystemClock;

// a synchronous Q with only one IRecyclable data
public class SimpleQueue<T extends IRecyclable> {

	private T mObject = null;
	private int mTotalObjects = 0;
	private int mDroppedObjects = 0;
	private boolean mbThreadWaiting;
	private long mScheduleTime = -1; // for event

	synchronized public final int getTotalObjects() {
		return mTotalObjects;
	}

	synchronized public final int getDroppedObjects() {
		return mDroppedObjects;
	}

	synchronized public final void clear() {
		mTotalObjects = 0;
		mDroppedObjects = 0;
		if (mObject != null) {
			mObject.recycle();
			mObject = null;
		}
	}

	synchronized public final void putObject(T object) {
		mTotalObjects++;
		if (mObject != null) {
			mObject.recycle();
			mDroppedObjects++;
		}
		mObject = object;
		if (mbThreadWaiting) {
			mbThreadWaiting = false;
			notifyAll();
		}
	}

	synchronized public final void postEvent(int event, int delay) {
		// TODO - event
		long scheduleTime = SystemClock.uptimeMillis() + delay;
		if (mScheduleTime < 0 || mScheduleTime > scheduleTime) {
			// re-schedule
			mScheduleTime = scheduleTime;
			if (mbThreadWaiting) {
				mbThreadWaiting = false;
				notifyAll();
			}
		}
	}

	synchronized public final T peekObject() {
		return mObject;
	}

	synchronized public final T waitForObject() throws InterruptedException {
		while (true) {
			if (mObject != null) {
				T object = mObject;
				mObject = null;
				return object;
			}

			mbThreadWaiting = true;
			wait();
		}
	}

	synchronized public final T waitForObjectAndEvent() throws InterruptedException {
		while (true) {
			if (mObject != null) {
				T object = mObject;
				mObject = null;
				return object;
			}

			if (mScheduleTime >= 0) {
				long currTime = SystemClock.uptimeMillis();
				if (currTime >= mScheduleTime) {
					mScheduleTime = -1;
					return null;
				}
				mbThreadWaiting = true;
				wait(mScheduleTime - currTime);
			} else {
				mbThreadWaiting = true;
				wait();
			}
		}
	}

	synchronized public final void dropObject() {
		if (mObject != null) {
			mObject.recycle();
			mObject = null;
			mDroppedObjects++;
		}
	}
}
