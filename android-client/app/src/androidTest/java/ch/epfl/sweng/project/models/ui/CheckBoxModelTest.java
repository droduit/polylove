package ch.epfl.sweng.project.models.ui;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Dominique on 26.10.2016.
 * Code for CheckBoxModel Class
 */
@RunWith(AndroidJUnit4.class)
public final class CheckBoxModelTest {

    private List<CheckBoxModel> cb;

    @Before
    public void setup() {
        cb = new ArrayList<>();
        cb.add(new CheckBoxModel("Suisse", 0, true));
        cb.add(new CheckBoxModel("France", 1, false));
    }

    @Test
    public void testName() {
        assertEquals(cb.get(0).getLabel(), "Suisse");
        assertEquals(cb.get(1).getLabel(), "France");
    }

    @Test
    public void testPosition() {
        assertEquals(cb.get(0).getPosition(), 0);
        assertEquals(cb.get(1).getPosition(), 1);
    }

    @Test
    public void testChecked() {
        assertTrue(!cb.get(1).isChecked());
        cb.get(1).setToggleChecked();
        assertTrue(cb.get(0).isChecked());
        assertTrue(cb.get(1).isChecked());
        assertTrue(!cb.get(1).isCheckedDefault());
    }

    @Test
    public void testToggleChecked() {
        cb.get(0).setToggleChecked();
        assertFalse(cb.get(0).isChecked());
    }

}
