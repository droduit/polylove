package ch.epfl.sweng.project.models.ui;

/**
 * Represents an Item of the swipe menu (drawerPane)
 * in the main activity of the application.
 *
 * @author Dominique Roduit
 */
public final class NavItemModel {

    private String title;
    private String subtitle;
    private int icon;
    private Class<?> expectedActivity;

    /**
     * Create an item of the swipe menu
     * @param title Title of the item
     * @param subtitle Subtitle of the item
     * @param icon Resource ID of the icon displayed to the left of the Title
     * @param expectedActivity Activity to display when item is clicked
     */
    public NavItemModel(final String title, final String subtitle,
                        final int icon, final Class<?> expectedActivity) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
        this.expectedActivity = expectedActivity;
    }

    /**
     * @return Title of the item
     */
    public String getTitle() {
        return title;
    }
    /**
     * @return Subtitle of the item
     */
    public String getSubtitle() {
        return subtitle;
    }
    /**
     * @return Resource ID of the icon displayed to the left of the Title
     */
    public int getIcon() {
        return icon;
    }

    /**
     * @return Activity to display when we click the item
     */
    public Class<?> getExpectedActivity() {
        return expectedActivity;
    }
}
