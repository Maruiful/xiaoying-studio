package com.xiaoying.pipeline;

/** 步骤按业务规则失败。reason 是给用户看的文案，会写入 task.fail_reason。 */
public class StepFailedException extends RuntimeException {

    public StepFailedException(String reason) {
        super(reason);
    }

    public String reason() {
        return getMessage();
    }
}
