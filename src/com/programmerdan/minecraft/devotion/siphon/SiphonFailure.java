package com.programmerdan.minecraft.devotion.siphon;

public class SiphonFailure extends RuntimeException {
	public SiphonFailure(String failure) {
		super(failure);
	}

	public SiphonFailure(Throwable failure) {
		super(failure);
	}

	public SiphonFailure(String failure, Throwable throwable) {
		super(failure, throwable);
	}
}
