package cz.bliksoft.javautils.fx.controls.images.cam;

import java.util.concurrent.Semaphore;

/**
 * Global mutual-exclusion lock around native camera device operations (Sarxos
 * {@code Webcam} enumeration/open/close and OpenCV {@code VideoCapture}
 * open/probe/release).
 *
 * <p>
 * Concurrent native access to camera devices from different threads (e.g. an
 * OpenCV resolution probe opening a device while Sarxos is still
 * enumerating/closing it) has been observed to crash the JVM
 * (EXCEPTION_ACCESS_VIOLATION inside {@code VideoCapture.set}). Serializing all
 * such operations through this lock avoids that.
 *
 * <p>
 * Backed by a {@link Semaphore} (not a {@code synchronized} block or
 * {@link java.util.concurrent.locks.ReentrantLock}) because a permit may be
 * acquired on one thread and released on another - see
 * {@link WebcamCameraSource#closeAsync}, where {@code Webcam.close()} runs on a
 * separate thread and can take ~20s (e.g. OBS Virtual Camera).
 */
public final class CameraNativeLock {

	private static final Semaphore PERMIT = new Semaphore(1);

	private CameraNativeLock() {
	}

	/** Acquires the lock, blocking until available. */
	public static void acquire() {
		try {
			PERMIT.acquire();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for camera device lock", e);
		}
	}

	/**
	 * Releases the lock. May be called from a different thread than
	 * {@link #acquire()}.
	 */
	public static void release() {
		PERMIT.release();
	}
}
