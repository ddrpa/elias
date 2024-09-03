package cc.ddrpa.dorian.elias.playground;

import cc.ddrpa.dorian.elias.core.annotation.EliasIgnore;
import cc.ddrpa.dorian.elias.core.annotation.EliasTable;
import cc.ddrpa.dorian.elias.core.annotation.types.UseText;

@EliasTable
public class AccountSetting extends BaseEntity {

    public Long id;

    @EliasIgnore
    protected String doNotSaveInDB;

    @UseText
    private String remark;
}