package com.askey.dvr.cdr7010.dashcam.domain;

public class MessageEvent<T>{
    private int code;
    private T data;

    public MessageEvent(int code) {
        this.code = code;
    }

    public MessageEvent(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MessageEvent{" +
                "code=" + code +
                ", data=" + data +
                '}';
    }
}