package com.arcsoft.sdk_demo.database;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = Database.class)
public class Report extends BaseModel{
    @Column
    @PrimaryKey(autoincrement = true)
    int id;

    @Column
    public String fileName;

    @Column
    public int age;

    @Column
    public String gender;

    @Column
    public int degree;

    @Column
    public String rect;
}
