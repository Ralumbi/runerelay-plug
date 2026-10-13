package com.runerelay;

import java.awt.Component;
import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import lombok.Value;

@Value
public class SystemSpecs
{
	String os;
	int cpuThreads;
	/**
	 * Total physical memory in MB, or -1 if the JVM doesn't expose it.
	 */
	long totalRamMb;
	long maxHeapMb;
	/**
	 * Physical display resolution.
	 */
	int screenWidth;
	int screenHeight;
	/**
	 * Display size after OS scaling (what the client actually lays out in).
	 */
	int logicalScreenWidth;
	int refreshRate;

	public static SystemSpecs detect(Component canvas)
	{
		Runtime runtime = Runtime.getRuntime();

		long ramMb = -1;
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		if (os instanceof com.sun.management.OperatingSystemMXBean)
		{
			@SuppressWarnings("deprecation")
			long bytes = ((com.sun.management.OperatingSystemMXBean) os).getTotalPhysicalMemorySize();
			ramMb = bytes / (1024 * 1024);
		}

		int width = 0;
		int height = 0;
		int logicalWidth = 0;
		int refresh = DisplayMode.REFRESH_RATE_UNKNOWN;
		if (!GraphicsEnvironment.isHeadless())
		{
			GraphicsConfiguration gc = canvas != null ? canvas.getGraphicsConfiguration() : null;
			GraphicsDevice device = gc != null
				? gc.getDevice()
				: GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			DisplayMode mode = device.getDisplayMode();
			width = mode.getWidth();
			height = mode.getHeight();
			refresh = mode.getRefreshRate();
			Rectangle bounds = device.getDefaultConfiguration().getBounds();
			logicalWidth = bounds.width;
		}

		return new SystemSpecs(
			System.getProperty("os.name", "Unknown"),
			runtime.availableProcessors(),
			ramMb,
			runtime.maxMemory() / (1024 * 1024),
			width,
			height,
			logicalWidth,
			refresh
		);
	}

	public boolean isRefreshRateKnown()
	{
		return refreshRate != DisplayMode.REFRESH_RATE_UNKNOWN && refreshRate > 0;
	}
}
