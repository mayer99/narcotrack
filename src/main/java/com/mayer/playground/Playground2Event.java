package com.mayer.playground;

import java.util.ArrayList;

public class Playground2Event extends PlaygroundEvent {

    static final ArrayList<Integer> test = new ArrayList<>();

    public Playground2Event() {
        test.add(20);
        test.add(21);
        test.add(22);
        System.out.println(test.toArray());
    }

    @Override
    ArrayList<Integer> getTest() {
        return test;
    }

}
