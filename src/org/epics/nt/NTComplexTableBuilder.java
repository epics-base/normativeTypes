/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.nt;

import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldBuilder;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.Union;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import java.util.ArrayList;

/**
 * Interface for in-line creating of NTComplexTable.
 *
 * One instance can be used to create multiple instances.
 * An instance of this object must not be used concurrently (an object has a state).
 * @author dgh
 */
public class NTComplexTableBuilder
{
    /**
     * Adds a column of a given Union to the NTComplexTable.
     *
     * @param name the name of the column
     * @param elementType the introspection type of the union array elements of the column
     * @return this instance of NTComplexTableBuilder
     */
    public NTComplexTableBuilder addColumn(String name, Union elementType)
    {
        // TODO: check for duplicate columns

        columnNames.add(name);
        types.add(elementType);

        return this;
    }

    /**
     * Adds a column of variant union type to the NTComplexTable.
     *
     * @param name the name of the column
     * @return this instance of NTComplexTableBuilder
     */
    public NTComplexTableBuilder addColumn(String name)
    {
        return add(name, variantUnion);
    }

    /**
     * Adds columns, each of a given Union, to the NTComplexTable.
     * 
     * @param names  the names of the columns
     * @param unions the types of the union elements of the columns
     * @return this instance of NTComplexTableBuilder
     */
    public NTComplexTableBuilder addColumns(String[] names, Union[] unions)
    {
        // TODO: check for duplicate columns

        if (names.length != unions.length)
            throw new RuntimeException("Column name and type lengths must match)");

        for (int i = 0; i < names.length; ++i)
            addColumn(names[i], unions[i]);

        return this;
    }

    /**
     * Adds columns, each of variant union type, to the NTComplexTable.
     * 
     * @param names  the names of the columns
     * @return this instance of NTComplexTableBuilder
     */
    public NTComplexTableBuilder addColumns(String[] names)
    {
        // TODO: check for duplicate columns

        for (int i = 0; i < names.length; ++i)
            addColumn(names[i], variantUnion);

        return this;
    }


    /**
     * Adds descriptor field to the NTComplexTable.
     * 
     * @return this instance of NTComplexTableBuilder
     */
    public NTComplexTableBuilder addDescriptor()
    {
        descriptor = true;
        return this;
    }

    /**
     * Adds alarm field to the NTComplexTable.
     * 
     * @return this instance of NTComplexTableBuilder
     */
    public NTComplexTableBuilder addAlarm()
    {
        alarm = true;
        return this;
    }

    /**
     * Adds timeStamp field to the NTComplexTable.
     * 
     * @return this instance of NTComplexTableBuilder
     */
    public NTComplexTableBuilder addTimeStamp()
    {
        timeStamp = true;
        return this;
    }

    /**
     * Creates a Structure that represents NTComplexTable.
     * This resets this instance state and allows new instance to be created.
     * 
     * @return a new instance of a Structure
     */
    public Structure createStructure()
    {
        FieldBuilder builder =
            FieldFactory.getFieldCreate().createFieldBuilder();

        FieldBuilder nestedBuilder = builder.
               setId(NTComplexTable.URI).
               addArray("labels", ScalarType.pvString).
               addNestedStructure("value");

        int len = columnNames.size();
        for (int i = 0; i < len; i++)
           nestedBuilder.addArray(columnNames.get(i), types.get(i));

        builder = nestedBuilder.endNested();

        NTField ntField = NTField.get();

        if (descriptor)
            builder.add("descriptor", ScalarType.pvString);

        if (alarm)
            builder.add("alarm", ntField.createAlarm());

        if (timeStamp)
            builder.add("timeStamp", ntField.createTimeStamp());

        int extraCount = extraFieldNames.size();
        for (int i = 0; i < extraCount; i++)
            builder.add(extraFieldNames.get(i), extraFields.get(i));

        Structure s = builder.createStructure();

        reset();
        return s;
    }

    /**
     * Creates a PVStructure that represents NTComplexTable.
     * This resets this instance state and allows new instance to be created
     *
     * @return a new instance of a PVStructure
     */
    public PVStructure createPVStructure()
    {
        // put the column names in labels by default
        String[] labelArray = columnNames.toArray(new String[columnNames.size()]);

        PVStructure s = PVDataFactory.getPVDataCreate().createPVStructure(
            createStructure());

        s.getSubField(PVStringArray.class, "labels").put(
            0, labelArray.length, labelArray, 0);

        return s;
    }

    /**
     * Creates an NTComplexTable instance.
     * This resets this instance state and allows new instance to be created
     *
     * @return a new instance of an NTComplexTable
     */
    public NTComplexTable create()
    {
        return new NTComplexTable(createPVStructure());
    }

    /**
     * Adds extra Field to the type.
     *
     * @param name the name of the field
     * @param field the field to add
     * @return this instance of NTComplexTableBuilder
     */
    public NTComplexTableBuilder add(String name, Field field) 
    {
        extraFields.add(field);
        extraFieldNames.add(name);
        return this;
    }

    NTComplexTableBuilder()
    {
        reset();
    }

    private void reset()
    {
        columnNames.clear();
        types.clear();
        descriptor = false;
        alarm = false;
        timeStamp = false;
        extraFieldNames.clear();
        extraFields.clear();
    }

    // NOTE: this preserves order, however it does not handle duplicates
    private ArrayList<String> columnNames = new ArrayList<String>();
    private ArrayList<Union> types = new ArrayList<Union>();

    private boolean descriptor;
    private boolean alarm;
    private boolean timeStamp;

    // NOTE: this preserves order, however it does not handle duplicates
    private ArrayList<String> extraFieldNames = new ArrayList<String>();
    private ArrayList<Field> extraFields = new ArrayList<Field>();

    static private Union variantUnion = FieldFactory.getFieldCreate().createVariantUnion();
}

