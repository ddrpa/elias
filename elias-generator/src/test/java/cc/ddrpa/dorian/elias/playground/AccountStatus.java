package cc.ddrpa.dorian.elias.playground;

import java.util.stream.Stream;

public enum AccountStatus {
    ACTIVE(1),
    INACTIVE(2),
    ;

    private final int code;

    AccountStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AccountStatus of(int code) {
        return Stream.of(AccountStatus.values())
            .filter(p -> p.getCode() == code)
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}
