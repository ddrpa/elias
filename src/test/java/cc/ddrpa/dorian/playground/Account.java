package cc.ddrpa.dorian.playground;

import cc.ddrpa.dorian.elias.annotation.GenerateTable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;

@GenerateTable
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
