package com.example.csci3130group1;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import com.example.csci3130group1.ui.search_for_tutorials.Tutorial;
import com.example.csci3130group1.ui.search_for_tutorials.TutorialAdapter;

public class Junit_US9 {

    private TutorialAdapter adapter;
    private List<Tutorial> tutorialList;

    @Before
    public void setUp() {
        // Create a list of sample tutorials
        tutorialList = new ArrayList<>();
        // Two tutorials are in Vancouver, one in Toronto.
        tutorialList.add(new Tutorial("Topic1", "10", "30", "Desc1", "Vancouver", "BC", "Canada", "Tutor1", "Degree1"));
        tutorialList.add(new Tutorial("Topic2", "20", "60", "Desc2", "Toronto", "ON", "Canada", "Tutor2", "Degree2"));
        tutorialList.add(new Tutorial("Topic3", "15", "45", "Desc3", "Vancouver", "BC", "Canada", "Tutor3", "Degree3"));

        // We can pass null as the Context because filtering doesn't depend on it.
        adapter = new TutorialAdapter(null, tutorialList);
    }

    @Test
    public void testEmptyFilterReturnsAll() {
        adapter.filter("", "", "");
        assertEquals("Empty filters should return all tutorials.", 3, adapter.getCount());
    }

}