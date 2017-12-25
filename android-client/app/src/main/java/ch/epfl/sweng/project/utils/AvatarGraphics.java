package ch.epfl.sweng.project.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.db.DBHandler;

/**
 * Provides a bunch of methods related to the build of a flattened Avatar bitmap.
 *
 * Will combine the different images composing the avatar
 * and given different size of a flattened resulting image.
 *
 * @author Christophe Badoux, Tim Nguyen
 */
public final class AvatarGraphics {

    private Avatar avatar;

    private String skinName = "";
    private String eyesName = "";
    private String baseName = "";
    private String hairName = "";
    private String shirtName = "";

    public AvatarGraphics(final Avatar avatar) {
        this.avatar = avatar;
    }

    public void setAvatar(final Avatar avatar) {
        this.avatar = avatar;
    }
    public Avatar getAvatar() {
        return this.avatar;
    }

    /**
     * Auxiliary function used to combine two images into a single one.
     * The images should have the same size.
     */
    public static Bitmap combineTwoImages(Bitmap underImg, Bitmap overImg) {
        int width = underImg.getWidth();
        int height = underImg.getHeight();

        Bitmap combination = Bitmap.createBitmap(width, height, underImg.getConfig());

        Canvas canvas = new Canvas(combination);

        canvas.drawBitmap(underImg, new Matrix(), null);
        canvas.drawBitmap(overImg, new Matrix(), null);

        return combination;
    }

    /*
    * Auxiliary function used to crop an image
    */
    public static Bitmap cropImage(Bitmap img, int startX, int startY, int lengthX, int lengthY) {
        Bitmap result = Bitmap.createBitmap(img, startX, startY, lengthX, lengthY);
        return result;
    }

    /**
     * Construct the avatar into a single image by taking each features and putting them together.
     */
    public Bitmap avatarSingleImage(Context context) {
        // we first convert our PNG images into Bitmap elements
        Resources res = context.getResources();
        String packageName = context.getPackageName();

        int  hairId = res.getIdentifier(hairName , "drawable", packageName);
        int  eyesId = res.getIdentifier(eyesName , "drawable", packageName);
        int  baseId = res.getIdentifier(baseName , "drawable", packageName);
        int  skinId = res.getIdentifier(skinName , "drawable", packageName);
        int  shirtId = res.getIdentifier(shirtName , "drawable", packageName);

        Bitmap hairColorAndStyle = BitmapFactory.decodeResource(res, hairId);
        Bitmap eyesColor = BitmapFactory.decodeResource(res, eyesId);
        Bitmap skinColor = BitmapFactory.decodeResource(res, skinId);
        Bitmap skinBase = BitmapFactory.decodeResource(res, baseId);
        Bitmap shirt = BitmapFactory.decodeResource(res, shirtId);

        int width = skinColor.getWidth();
        int height = skinColor.getHeight();

        Bitmap withSkin = Bitmap.createBitmap(skinColor, 0, 0, width, height);

        Bitmap withEyes = AvatarGraphics.combineTwoImages(withSkin, eyesColor);
        Bitmap withBase = AvatarGraphics.combineTwoImages(withEyes, skinBase);
        Bitmap withShirt = AvatarGraphics.combineTwoImages(withBase, shirt);
        Bitmap withHair = AvatarGraphics.combineTwoImages(withShirt, hairColorAndStyle);

        return withHair;
    }

    public void updateImage(Context context) {

        DBHandler db = DBHandler.getInstance(context);

        initializeNameWithGender("all");

        getBaseImage();
        getHairImage();
        getSkinImage();
        getEyesImage();
        getShirtImage();

        // combine each avatar's features into a single Bitmap
        Bitmap image = avatarSingleImage(context);
        avatar.setImage(image);

        // create the avatar's icon
        Bitmap avatarIconBackground = BitmapFactory.decodeResource(context.getResources(),
                AndroidUtils.getImageId(context, "avatar_icon_background"));

        Bitmap avatarIconResized, croppedImage;

        int width = image.getWidth();
        int height = image.getHeight();

        if(avatar.getGender() == Profile.Gender.Female) {
            croppedImage = AvatarGraphics.cropImage(image, width/5, (int) (height*0.07), height/2, height/2);
        } else {
            croppedImage = AvatarGraphics.cropImage(image, (int) (0.07*width), 0, height/2, height/2);
        }
        avatarIconResized = Bitmap.createScaledBitmap(croppedImage, 336, 336, false);

        Bitmap icon = AvatarGraphics.combineTwoImages(avatarIconBackground, avatarIconResized);
        avatar.setIcon(icon);

        if(avatarIconResized == null) {
            System.out.println("Error. Can't resize the avatar.");
        }
        if(icon == null) {
            System.out.println("Error on avatar icon. Can't create an icon");
        }
        if(db != null){
            db.storeAvatar(avatar);
        }
    }


    /**
     * Function used to initialize the the name of each feature of the avatar so it can be used
     * to set image resources.
     */
    public void initializeNameWithGender(String mode) {
        if (avatar.getGender() == Profile.Gender.Female) {
            switch(mode) {
                case "skin":
                    skinName = "girl_";
                    break;
                case "eyes":
                    eyesName = "girl_";
                    break;
                case "base":
                    baseName = "girl_";
                    break;
                case "hair":
                    hairName = "girl_";
                    break;
                case "shirt":
                    shirtName = "girl_";
                    break;
                default:
                    skinName = "girl_";
                    eyesName = "girl_";
                    baseName = "girl_";
                    hairName = "girl_";
                    shirtName = "girl_";
                    break;
            }
        } else {
            switch(mode) {
                case "skin":
                    skinName = "boy_";
                    break;
                case "eyes":
                    eyesName = "boy_";
                    break;
                case "base":
                    baseName = "boy_";
                    break;
                case "hair":
                    hairName = "boy_";
                    break;
                case "shirt":
                    shirtName = "boy_";
                    break;
                default:
                    skinName = "boy_";
                    eyesName = "boy_";
                    baseName = "boy_";
                    hairName = "boy_";
                    shirtName = "boy_";
                    break;
            }
        }
    }

    public String getBaseImage() {
        initializeNameWithGender("base");

        if (baseName.equals("girl_") || baseName.equals("boy_")) {
            baseName += "skin_base";
        } else {
            System.out.println("Error on gender. Can't set the avatar's base");
        }
        return baseName;
    }

    // Set the avatar's eyes
    public String getEyesImage() {
        initializeNameWithGender("eyes");

        if (eyesName.equals("girl_") || eyesName.equals("boy_")) {
            eyesName += "eyes_" + avatar.getEyeColor().toString().toLowerCase();
        } else {
            System.out.println("Error on gender. Can't change the eye color.");
        }
        return eyesName;
    }

    // Set the avatar's skin tone
    public String getSkinImage() {
        initializeNameWithGender("skin");

        if (skinName.equals("girl_") || skinName.equals("boy_")) {
            skinName += "skin_" + avatar.getSkinTone().toString().toLowerCase();
        } else {
            System.out.println("Error on gender. Can't change the skin tone.");
        }
        return skinName;
    }

    // Set both avatar's hair and style
    public String getHairImage() {
        initializeNameWithGender("hair");

        if (hairName.equals("girl_") || hairName.equals("boy_")) {
            hairName += "hair_" + avatar.getHairStyle().toString().toLowerCase() +
                    "_" + avatar.getHairColor().toString().toLowerCase();
        } else {
            System.out.println("Error on gender. Can't change the hair color.");
        }
        return hairName;
    }

    public String getShirtImage() {
        initializeNameWithGender("shirt");

        if (shirtName.equals("girl_") || shirtName.equals("boy_")) {
            shirtName += "shirt_" + avatar.getShirt().toString().toLowerCase();
        } else {
            System.out.println("Error on gender. Can't change the shirt.");
        }
        return shirtName;
    }
}
