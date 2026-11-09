package com.fanlink.ocr;

public interface OcrClient {
	OcrResult extractText(byte[] imageBytes, String filename);
}
