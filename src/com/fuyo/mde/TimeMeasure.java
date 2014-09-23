package com.fuyo.mde;

public class TimeMeasure {
	private long startTime;
	private long totalTime;
	private boolean started; 
	private int interruptCount;
	public TimeMeasure() {
		startTime = 0;
		totalTime = 0;
		interruptCount = 0;
		started = false;
	}
	public void start() {
		if (!started) {
			startTime = System.currentTimeMillis();
			started = true;
		}
	}
	public void stop() {
		if (started) {
			long endTime = System.currentTimeMillis();
			totalTime += (endTime - startTime);
			started = false;
		}
	}
	public long getTime() { 
		return totalTime;
	}
}
