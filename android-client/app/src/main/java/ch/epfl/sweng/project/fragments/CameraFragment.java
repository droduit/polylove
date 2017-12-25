package ch.epfl.sweng.project.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.camera.CropImageIntentBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.utils.MediaStoreUtils;

/**
 * Fragment whose purpose is to display an area to choose an image either
 * from gallery or directly from camera.
 * The user can choose its picture, and then crop it with ratio 1:1.
 * Once an image is chosen, and cropped, she is stored in a Bitmap format variable
 * and can be retrieve from the parent activity, by calling the public getter getBitmapImg().
 *
 * @author Dominique Roduit
 */
public final class CameraFragment extends Fragment {

    // Dimensions {x,y} of the final cropped image
    private static final int[] IMG_OUTPUT_DIM = new int[]{500, 500};

    // Code of intents called
    private static final int REQUEST_CAMERA = 41;
    private static final int REQUEST_GALLERY = 42;
    private static final int REQUEST_CROP_PICTURE = 43;

    // ImageView who display the chosen cropped image or a default image
    private ImageView thumbPhoto;
    // Parent activity of this fragment
    private FragmentActivity activity;
    // To store the user's chosen cropped image
    private Bitmap bitmapImg = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        DBHandler db = DBHandler.getInstance(getActivity().getApplicationContext());
        long userId = Settings.getInstance(getActivity().getApplicationContext()).getUserID();

        Profile profile = db.getProfile(userId);

        activity = getActivity();
        thumbPhoto = (ImageView)view.findViewById(R.id.thumbPhoto);

        if(!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // User do not have a camera
            view.setEnabled(false);
            view.setAlpha(0.4F);
        }

        final List<String> actions = new ArrayList<>();
        actions.add(getString(R.string.from_camera));
        actions.add(getString(R.string.from_gallery));

        if(profile != null) {
            if(profile.getPhoto() != null) {
                bitmapImg = profile.getPhoto();
            }
        }

        thumbPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(bitmapImg != null) {
                if (actions.size() < 3) {
                    actions.add(getString(R.string.delete_photo));
                }
            } else {
                if(actions.size() == 3) {
                    actions.remove(2);
                }
            }

            String[] actionsArray = new String[actions.size()];

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setItems(actions.toArray(actionsArray), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int index) {
                performAction(index);
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            }
        });

        return view;
    }

    private void showToast(int stringId) {
        Toast.makeText(activity, getString(stringId), Toast.LENGTH_SHORT).show();
    }

    private void performAction(int index) {
        switch(index) {
            // Take picture from Camera -----------------------------------
            case 0:
                startCamera();
                break;

            // Take picture from gallery ----------------------------------
            case 1:
                startGallery();
            break;

            // Delete current picture -------------------------------------
            case 2:
                bitmapImg = null;
                thumbPhoto.setScaleType(ImageView.ScaleType.CENTER);
                thumbPhoto.setImageResource(R.drawable.ic_local_see_black_24dp);
            break;
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("camera", String.valueOf(resultCode)+ " - "+getActivity().getFilesDir());

        File croppedImageFile = new File(getActivity().getFilesDir(), "profilePicture.jpg");

        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case REQUEST_CAMERA :
                    startGallery();
                    //performCrop(croppedImageFile, data);
                break;

                case REQUEST_GALLERY :
                    performCrop(croppedImageFile, data);
                break;

                case REQUEST_CROP_PICTURE :
                    Bitmap croppedPic = BitmapFactory.decodeFile(croppedImageFile.getAbsolutePath());
                    bitmapImg = croppedPic;
                    thumbPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    thumbPhoto.setImageBitmap(croppedPic);
                break;
            }
        }
    }


    private void performCrop(File croppedImageFile, Intent data) {
        Uri croppedImage = Uri.fromFile(croppedImageFile);

        CropImageIntentBuilder cropImage = new CropImageIntentBuilder(IMG_OUTPUT_DIM[0], IMG_OUTPUT_DIM[1], croppedImage);
        cropImage.setOutlineColor(ContextCompat.getColor(getActivity(), R.color.colorAccent));
        cropImage.setDoFaceDetection(true);
        cropImage.setSourceImage(data.getData());

        startActivityForResult(cropImage.getIntent(getActivity()), REQUEST_CROP_PICTURE);
    }

    private void startGallery() {
        try {
            startActivityForResult(
                    MediaStoreUtils.getGalleryIntent(),
                    REQUEST_GALLERY
            );
        } catch(ActivityNotFoundException e){
            showToast(R.string.gallery_not_supported);
        }
    }

    private void startCamera() {
        try {
            startActivityForResult(
                    MediaStoreUtils.getCameraIntent(),
                    REQUEST_CAMERA
            );
        } catch(ActivityNotFoundException e){
            showToast(R.string.capturing_not_supported);
        }
    }

    public Bitmap getBitmapImg() {
        return bitmapImg;
    }
}
