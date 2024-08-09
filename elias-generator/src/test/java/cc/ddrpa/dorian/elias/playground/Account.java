package cc.ddrpa.dorian.elias.playground;

import cc.ddrpa.dorian.elias.annotation.EliasTable;
import cc.ddrpa.dorian.elias.annotation.Index;
import cc.ddrpa.dorian.elias.annotation.types.TypeOverride;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@EliasTable(
    enable = true,
    indexes = {
        @Index(columnList = "email_address", unique = true),
        @Index(columnList = "username"),
        @Index(columnList = "username, email_address", unique = true),
    }
)
@TableName("tbl_account")
public class Account {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("username")
    @NotNull
    private String name;
    @NotBlank
    private String emailAddress;
    @TypeOverride(type = "varchar", length = 500)
    private LocalDate createTime;
    private AccountStatus accountStatus;
    private byte[] avatar;
}
