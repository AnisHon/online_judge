package com.anishan.user.domain.vo;

import com.anishan.commons.domain.R;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel("用于返回只有二元结果的类，可以附加一些信息")
@AllArgsConstructor
@NoArgsConstructor
public class BinaryResultOv {

    private boolean success;
    private String message;

    public R<BinaryResultOv> tOvR() {
        return R.success(this);
    }

    public static BinaryResultOv success(String msg) {
        return new BinaryResultOv(true, msg);
    }

    public static BinaryResultOv fail(String msg) {
        return new BinaryResultOv(false, msg);
    }

    public static BinaryResultOv ternary(boolean success, String msgSuccess, String msgFail) {
        return new BinaryResultOv(success, success ? msgSuccess : msgFail);
    }
}
