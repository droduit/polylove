package ch.epfl.sweng.project.models.ui;

/**
 * Represents the Model for a single row
 * with a checkbox and a text, to be used as
 * an item of an Adapter in a listView.
 *
 * @author Dominique Roduit
 */
public final class CheckBoxModel {

    /* Text displayed next to the checkbox */
    private String label;

    /* Hold the position of this row in the adapter */
    private int position;

    /* Inform whether the checkbox is automatically
       checked by default or not */
    private boolean checkedDefault;

    /* Inform whether the checkbox was checked by
       the user in the Adapter or not */
    private boolean isChecked;

    /**
     * Create an Item composed by a checkbox with a text.
     * To be used in ListView Adapter.
     * @param label Text displayed next to the checkbox
     * @param position Position of this item in the adapter
     * @param checkedDefault Inform whether the checkboy is automatically checked by default or not
     */
    public CheckBoxModel(String label, int position, boolean checkedDefault) {
        this.label = label;
        this.position = position;
        this.checkedDefault = checkedDefault;
        this.isChecked = checkedDefault;
    }

    /**
     * @return Text associated to the checkbox
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * @return Position of the checkbox item in the ListView Adapter
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * @return true if the checkbox is checked by default
     */
    public boolean isCheckedDefault() { return this.checkedDefault; }

    /**
     * Set the checkbox as checked if it was not and inversely
     */
    public void setToggleChecked() {
        isChecked = !isChecked;
    }

    /**
     * @return Whether the checkbox is checked or not
     */
    public boolean isChecked() { return this.isChecked; }
}
