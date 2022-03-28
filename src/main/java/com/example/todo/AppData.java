package com.example.todo;

import java.io.Serializable;
import java.util.ArrayList;

public class AppData implements Serializable {
    private final ArrayList<TodoTask> list;
    private final ArrayList<TodoTask> listDone;

    public AppData(ArrayList<TodoTask> list, ArrayList<TodoTask> listDone) {
        this.list = list;
        this.listDone = listDone;
    }

    public ArrayList<TodoTask> getList() {
        return list;
    }

    public ArrayList<TodoTask> getListDone() {
        return listDone;
    }
}
