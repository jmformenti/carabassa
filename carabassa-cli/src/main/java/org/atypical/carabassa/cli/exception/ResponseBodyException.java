package org.atypical.carabassa.cli.exception;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ResponseBodyException {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
	private ZonedDateTime timestamp;
	private String status;
	private String message;
	private String path;

	public ResponseBodyException() {
		super();
	}

	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
