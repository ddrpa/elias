package cc.ddrpa.dorian.elias.playground;

import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable.Index;
import cc.ddrpa.dorian.elias.core.annotation.types.TypeOverride;
import cc.ddrpa.dorian.elias.core.annotation.types.UseText;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
    @UseText
    private String biography;
    private boolean isActivated;
    /**
     * 账户余额，这里是为了演示保留小数 实际如果是货币之类的数据，建议以业务系统处理的最小单位（如分）为单位一
     */
    private Number balance;
}
