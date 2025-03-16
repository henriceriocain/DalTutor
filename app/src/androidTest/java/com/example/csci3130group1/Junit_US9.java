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

    @Test
    public void testLocationFilter() {
        adapter.filter("Vanc", "", "");
        assertEquals("Filter by location 'Vanc' should return 2 tutorials.", 2, adapter.getCount());
    }

    @Test
    public void testFeeFilter() {
        adapter.filter("", "15", "");
        assertEquals("Filter by fee <= 15 should return 2 tutorials.", 2, adapter.getCount());
    }

    @Test
    public void testDurationFilter() {
        adapter.filter("", "", "50");
        assertEquals("Filter by duration < 50 should return 2 tutorials.", 2, adapter.getCount());
    }

    @Test
    public void testCombinedFilter() {
        adapter.filter("Vanc", "15", "50");
        // Only the tutorials in Vancouver with fee <= 15 and duration < 50 pass.
        assertEquals("Combined filter should return 2 tutorials.", 2, adapter.getCount());
    }

    @Test
    public void testNullCityTutorial() {

        tutorialList.add(new Tutorial("Topic4", "10", "20", "Desc4", null, "BC", "Canada", "Tutor4", "Degree4"));
        adapter.updateTutorials(tutorialList);
        adapter.filter("Vanc", "", "");
        assertEquals("Tutorial with null city should be excluded from location filter.", 2, adapter.getCount());
    }
}
