package ch.epfl.sweng.project.models;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.utils.AndroidUtils;
import ch.epfl.sweng.project.utils.AvatarGraphics;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for Avatar class
 *
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class AvatarTest {

    private Avatar avatar1;
    private Avatar avatar2;
    private Context context;

    @Before
    public void setup() throws Exception{
        avatar1 = new Avatar(1, Profile.Gender.Male, Avatar.Eye.Green, Avatar.HairColor.Blond, Avatar.HairStyle.Style1, Avatar.Skin.Light, Avatar.Shirt.Style1);
        avatar2 = new Avatar(1, Profile.Gender.Female, Avatar.Eye.Blue, Avatar.HairColor.Ginger, Avatar.HairStyle.Style2, Avatar.Skin.Dark, Avatar.Shirt.Style2);
        context = getTargetContext();
    }
    
    @Test
    public void toJsonTest() throws JSONException {
        JSONObject jsonObject = avatar1.toJson();
        assertEquals(avatar1.getEyeColor(), Avatar.Eye.valueOf(jsonObject.getString("eyes")));
    }

    @Test
    public void fromJsonTest() throws JSONException {
        JSONObject jsonObject = avatar1.toJson();
        jsonObject.put("id", 1L);
        Avatar avatarFromJSON = Avatar.fromJSON(jsonObject);
        assertEquals(avatar1.getEyeColor(), avatarFromJSON.getEyeColor());

        assertNull(Avatar.fromJSON(null));
    }

    @Test
    public void imageTest(){
        Resources res = context.getResources();
        String packageName = context.getPackageName();

        int  hairId = res.getIdentifier("boy_hair_style1_blond" , "drawable", packageName);
        int  eyesId = res.getIdentifier("boy_eyes_green" , "drawable", packageName);
        int  baseId = res.getIdentifier("boy_skin_base" , "drawable", packageName);
        int  skinId = res.getIdentifier("boy_skin_light" , "drawable", packageName);
        int  shirtId = res.getIdentifier("boy_shirt_style1" , "drawable", packageName);

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

        Bitmap avatarIconBackground = BitmapFactory.decodeResource(context.getResources(),
                AndroidUtils.getImageId(context, "avatar_icon_background"));

        Bitmap avatarIconResized, croppedImage;

        width = withHair.getWidth();
        height = withHair.getHeight();

        croppedImage = AvatarGraphics.cropImage(withHair, (int) (0.07*width), 0, height/2, height/2);
        avatarIconResized = Bitmap.createScaledBitmap(croppedImage, 336, 336, false);

        Bitmap icon = AvatarGraphics.combineTwoImages(avatarIconBackground, avatarIconResized);

        assertTrue(avatar1.getImage(context).sameAs(withHair));
        assertTrue(avatar1.getIcon(context).sameAs(icon));

        hairId = res.getIdentifier("girl_hair_style2_ginger" , "drawable", packageName);
        eyesId = res.getIdentifier("girl_eyes_blue" , "drawable", packageName);
        baseId = res.getIdentifier("girl_skin_base" , "drawable", packageName);
        skinId = res.getIdentifier("girl_skin_dark" , "drawable", packageName);
        shirtId = res.getIdentifier("girl_shirt_style2" , "drawable", packageName);

        hairColorAndStyle = BitmapFactory.decodeResource(res, hairId);
        eyesColor = BitmapFactory.decodeResource(res, eyesId);
        skinColor = BitmapFactory.decodeResource(res, skinId);
        skinBase = BitmapFactory.decodeResource(res, baseId);
        shirt = BitmapFactory.decodeResource(res, shirtId);

        width = skinColor.getWidth();
        height = skinColor.getHeight();

        withSkin = Bitmap.createBitmap(skinColor, 0, 0, width, height);

        withEyes = AvatarGraphics.combineTwoImages(withSkin, eyesColor);
        withBase = AvatarGraphics.combineTwoImages(withEyes, skinBase);
        withShirt = AvatarGraphics.combineTwoImages(withBase, shirt);
        withHair = AvatarGraphics.combineTwoImages(withShirt, hairColorAndStyle);

        assertTrue(avatar2.getImage(context).sameAs(withHair));
    }

    private Bitmap combineTwoImages(Bitmap underImg, Bitmap overImg) {
        int width = underImg.getWidth();
        int height = underImg.getHeight();

        Bitmap combination = Bitmap.createBitmap(width, height, underImg.getConfig());

        Canvas canvas = new Canvas(combination);

        canvas.drawBitmap(underImg, new Matrix(), null);
        canvas.drawBitmap(overImg, new Matrix(), null);

        return combination;
    }

    private Bitmap cropImage(Bitmap img, int startX, int startY, int lengthX, int lengthY) {
        Bitmap result = Bitmap.createBitmap(img, startX, startY, lengthX, lengthY);
        return result;
    }
}