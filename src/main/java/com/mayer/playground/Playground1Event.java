package com.mayer.playground;

import java.util.ArrayList;

public class Playground1Event extends PlaygroundEvent {

    static final ArrayList<Integer> test = new ArrayList<>();

    public Playground1Event() {
        test.add(10);
        test.add(11);
        test.add(12);
        System.out.println(test);
    }

    @Override
    ArrayList<Integer> getTest() {
        return test;
    }

}
