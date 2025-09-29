package cc.ddrpa.dorian.elias.playground;

import com.baomidou.mybatisplus.annotation.IEnum;

import java.util.stream.Stream;

public enum AccountStatus implements IEnum<Integer> {
    ACTIVE(1),
    INACTIVE(20),
    ;

    private final int code;

    AccountStatus(int code) {
        this.code = code;
    }

    public static AccountStatus of(int code) {
        return Stream.of(AccountStatus.values())
                .filter(p -> p.getCode() == code)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public int getCode() {
        return code;
    }

    @Override
    public Integer getValue() {
        return code;
    }
}
