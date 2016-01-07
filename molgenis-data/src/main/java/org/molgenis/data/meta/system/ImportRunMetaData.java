package org.molgenis.data.meta.system;

import org.molgenis.data.support.DefaultEntityMetaData;

import java.util.Arrays;

import static org.molgenis.MolgenisFieldTypes.DATETIME;
import static org.molgenis.MolgenisFieldTypes.ENUM;
import static org.molgenis.MolgenisFieldTypes.TEXT;
import static org.molgenis.MolgenisFieldTypes.INT;

public class ImportRunMetaData extends DefaultEntityMetaData {
    public static final String ID = "id";
    public static final String STARTDATE = "startDate";
    public static final String ENDDATE = "endDate";
    public static final String USERNAME = "userName";
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final String PROGRESS = "progress";
    public static final String IMPORTEDENTITIES = "importedEntities";

    public ImportRunMetaData() {
        super("importRun");
        addAttribute(ID).setAuto(true).setVisible(false)
                .setDescription("automatically generated internal id, only for internal use.");
        addAttribute(STARTDATE).setDataType(DATETIME).setNillable(false);
        addAttribute(ENDDATE).setDataType(DATETIME).setNillable(true);
        addAttribute(USERNAME).setNillable(false);
        addAttribute(STATUS).setDataType(ENUM).setNillable(false).setEnumOptions(Arrays.asList("RUNNING,FINISHED,FAILED"));
        addAttribute(MESSAGE).setDataType(TEXT).setNillable(true);
        addAttribute(PROGRESS).setDataType(INT).setNillable(false);
        addAttribute(IMPORTEDENTITIES).setDataType(TEXT).setNillable(true);
    }
}
