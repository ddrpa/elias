package cc.ddrpa.dorian.playground;

import cc.ddrpa.dorian.elias.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.annotation.GenerateTable;
import cc.ddrpa.dorian.elias.annotation.types.AsLongText;

@GenerateTable
public class AccountSetting extends BaseEntity {

    public Long id;

    @EliasIgnore
    protected String doNotSaveInDB;

    @AsLongText
    private String remark;
}