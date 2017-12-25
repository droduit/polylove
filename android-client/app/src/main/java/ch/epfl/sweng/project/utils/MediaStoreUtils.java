package ch.epfl.sweng.project.utils;

import android.content.Intent;
import android.provider.MediaStore;

/**
 * Provide methods to get the Media Intent needed
 * like camera, gallery, etc ...
 * @author Dominique Roduit
 */
public final class MediaStoreUtils {
    private MediaStoreUtils() {}

    /**
     * @return Intent of the gallery to choose an image
     */
    public static Intent getGalleryIntent() {
        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return intent;
    }

    /**
     * @return Native intent of the camera
     */
    public static Intent getCameraIntent() {
        final Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        return intent;
    }
}
