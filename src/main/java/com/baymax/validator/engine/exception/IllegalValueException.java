/**
 * 
 */
package com.baymax.validator.engine.exception;


import com.baymax.validator.engine.IBuilder;

/**
 * @author xiao.hu
 *
 */
public class IllegalValueException extends IllegalArgumentException {
	private static final long serialVersionUID = 8029525230455039303L;

	private String errorCode = null;
	private String key = null;
	private Object value = null;

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {
		return String.format("AppException{errorCode='%s', parameters='%s=%s'}",
				errorCode, key, value);
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	@Override
	public String getMessage() {
		return this.errorCode;
	}


	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public static class Builder implements IBuilder<IllegalValueException> {
		private IllegalValueException exception = new IllegalValueException();

		@Override
		public IllegalValueException build() {
			return this.exception;
		}

		public Builder errorCode(String errorCode) {
			exception.setErrorCode(errorCode);
			return this;
		}

		public Builder params(String key, Object value) {
			exception.setKey(key);
			exception.setValue(value);
			return this;
		}
	}
}
