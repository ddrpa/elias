package cc.ddrpa.dorian.elias.playground;

import cc.ddrpa.dorian.elias.annotation.EliasTable;
import cc.ddrpa.dorian.elias.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.annotation.types.AsLongText;

@EliasTable
public class AccountSetting extends BaseEntity {

    public Long id;

    @EliasIgnore
    protected String doNotSaveInDB;

    @AsLongText
    private String remark;
}