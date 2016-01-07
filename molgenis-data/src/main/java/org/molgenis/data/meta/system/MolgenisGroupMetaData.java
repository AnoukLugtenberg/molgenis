package org.molgenis.data.meta.system;

import org.molgenis.data.support.DefaultEntityMetaData;
import static org.molgenis.MolgenisFieldTypes.BOOL;

public class MolgenisGroupMetaData extends DefaultEntityMetaData {
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String ACTIVE = "active";

    public MolgenisGroupMetaData() {
        super("molgenisGroup");
        addAttribute(ID).setAuto(true).setVisible(false)
                .setDescription("automatically generated internal id, only for internal use.");
        addAttribute(NAME).setLabel("Name");
        addAttribute(ACTIVE).setLabel("Active").setDataType(BOOL).setDefaultValue("true").setDescription("Boolean to indicate whether this group is in use.");
    }
}
