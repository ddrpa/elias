package cc.ddrpa.dorian.playground;

import cc.ddrpa.dorian.elias.annotation.GenerateTable;
import cc.ddrpa.dorian.elias.annotation.Index;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@GenerateTable(
    enable = true,
    indexes = {
        @Index(columnList = "email_address", unique = true),
        @Index(columnList = "username, email_address", unique = true),
    }
)
@TableName("tbl_account")
public class Account {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("username")
    private String name;
    private String emailAddress;
    private LocalDate createTime;
    private AccountStatus accountStatus;
    private byte[] avatar;
}
