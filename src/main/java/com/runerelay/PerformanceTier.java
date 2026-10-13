package com.runerelay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PerformanceTier
{
	AUTO("Auto-detect"),
	LOW("Low"),
	BALANCED("Balanced"),
	HIGH("High"),
	ULTRA("Ultra");

	private final String displayName;

	/**
	 * Guess a tier from the hardware we can see. The graphics card can't be queried
	 * without native code, so this never picks ULTRA; users opt into that themselves.
	 */
	public static PerformanceTier detect(SystemSpecs specs)
	{
		int threads = specs.getCpuThreads();
		long ramGb = specs.getTotalRamMb() / 1024;
		boolean ramKnown = specs.getTotalRamMb() > 0;

		if (threads <= 4 || (ramKnown && ramGb < 8))
		{
			return LOW;
		}
		if (threads >= 12 && (!ramKnown || ramGb >= 16))
		{
			return HIGH;
		}
		return BALANCED;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
