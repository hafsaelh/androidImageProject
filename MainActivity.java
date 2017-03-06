package com.example.mawdoudbacar.imageproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

import static android.R.attr.bitmap;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {


    Button bPhoto, bSelection;

    SeekBar seekbarLuminosity,seekbarContrast;

    TextView textLuminosity,textContrast;

    private Bitmap bmp, bmpSave, operation;
    ImageView myImageView;

    int maxLum, progressIntLuminosity, maxContrast, progressIntContrast;

    private static int RESULT_LOAD_IMG = 1;

    private static final int CAMERA_REQUEST = 1888;

    //Prendre des photos depuis la caméra

    static String Camera_Photo_ImagePath = "";
    private static File f;
    private static int Take_Photo = 2;
    static String Camera_Photo_ImageName = "";
    public static String SaveFolderName;
    private static File gallery;

        //Zoom 

    private static final String TAG = "Touch";
    @SuppressWarnings("unused")
    private static final float MIN_ZOOM = 1f,MAX_ZOOM = 1f;
    
    // Ces matrices seront utilisées pour enregistrer les pixels de l'image

    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    
    // Les 3 états (events) que l'utilisateur essaie d'exécuter

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;
    
    //Ces objets PointF sont utilisés pour enregistrer le(s) point(s) que l'utilisateur touche 
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        myImageView = (ImageView) findViewById(R.id.lenna);
        final Bitmap b = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.lenna);
        bmp = b.copy(Bitmap.Config.ARGB_8888, true);
        bmpSave = b.copy(Bitmap.Config.ARGB_8888, true);

        bSelection = (Button)findViewById(R.id.bSelection);
        bPhoto = (Button)findViewById(R.id.bPhoto);
        seekbarLuminosity = (SeekBar)findViewById(R.id.seekBarLuminosity);
        seekbarContrast = (SeekBar)findViewById(R.id.seekBarContrast);
        textLuminosity = (TextView)findViewById(R.id.textLuminosity);
        textContrast = (TextView)findViewById(R.id.textContrast);


        // Permissions d'accès au téléphone pour récupérer les images
        final int REQUEST_EXTERNAL_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };



        bPhoto.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onClick(View v){

                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{Manifest.permission.CAMERA
                            },
                            CAMERA_REQUEST);
                }else {
         // créer un dossier pour contenir les photos prises
                SaveFolderName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/App Cam";
                gallery = new File(SaveFolderName);
                if (!gallery.exists())
                    gallery.mkdirs();
        // enregistrer les photos prises 
                Camera_Photo_ImageName = "Photo" + ".jpg";
                Camera_Photo_ImagePath = SaveFolderName + "/" + "PictureApp" + ".jpg";
                System.err.println(" Camera_Photo_ImagePath  "+ Camera_Photo_ImagePath);
                f = new File(Camera_Photo_ImagePath);
                startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f)), Take_Photo);
                System.err.println("f" + f);
                }
            }
        });


        bSelection.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onClick(View v){

                /*Vérification des permissions, si la permission n'est pas accordé pour accéder à la
                galerie on la demande, si l'utilisateur n'as pas accordé l'autorisation on ne peut pas
                y accéder tant qu'il ne mettera pas oui
                */

                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                            },
                            REQUEST_EXTERNAL_STORAGE);
                }else {

                    loadImagefromGallery(v);
                }
            }
        });


        maxContrast = 200;
        seekbarContrast.setMax(maxContrast);
        seekbarContrast.setProgress(maxContrast/2);
        progressIntContrast = maxContrast/2;
        
   // Création d'une seekBar pour le réglage du contraste
        seekbarContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int progressChange = progress;
                contrast(bmp, progressChange - progressIntContrast);
                progressIntContrast = progressChange;


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        maxLum = getMax(bmp);
        seekbarLuminosity.setMax(maxLum);

        seekbarLuminosity.setProgress(maxLum/2);
        progressIntLuminosity = maxLum / 2;
        
 // Création d'une seekBar pour le réglage de la luminosité
        seekbarLuminosity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int progressChange = progress;
                brightnessSeek(bmp, progressChange - progressIntLuminosity);
                progressIntLuminosity = progressChange;


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        myImageView.setOnTouchListener((View.OnTouchListener) this);


    }
    // Création d'un menu.xml
    public boolean onCreateOptionsMenu(Menu menu) {

        //Création d'un MenuInflater qui va permettre d'instancier un Menu XML en un objet Menu
        MenuInflater inflater = getMenuInflater();
        //Instanciation du menu.xml XML spécifier en un objet Menu
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_quit:
                finish();
                return true;

            case R.id.item_save:
                MediaStore.Images.Media.insertImage(getContentResolver(), bmp, "Bmp modified" ,"Test");
                return true;

            case R.id.item_gray:
                toGray(bmp);
                return true;

            case R.id.item_grayFast:
                toGray2(bmp);
                return true;

            case R.id.item_sepia:
                sepia(bmp);
                return true;

            case R.id.item_invert:
                invert(bmp);
                return true;

            case R.id.item_brightnessPlus:
                brightnessPlus(bmp,5);
                return true;

            case R.id.item_brightnessMoins:
                brightnessMoins(bmp, -5);
                return true;

            case R.id.item_reset:
                raz();
                return true;

            case R.id.item_randomshade:
                toColorize(bmp);
                return true;

            case R.id.item_contrast:
                contrast(bmp);
                return true;

            case R.id.item_egalHisto:
                egalHistogram(bmp);
                return true;

            case R.id.item_egalImgCouleur:
                egalImgCouleur(bmp);
                return true;

            case R.id.item_Convolution:
                convolution(bmp);
                return true;

            case R.id.item_test:
                Intensity(bmp);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }




    /* Les deux fonctions suivantes permettent de récupérer une image depuis la galerie
* et de l'affecter à la view pour pouvoir ensuite effectuer les différents traitement*/

    public void loadImagefromGallery(View view) {
        // Création d'un intent pour récupérer une image depuis la galerie
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Commencement de l'intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        try {

            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK
                    && null != data) { // Si l'image est récupérée depuis la galerie

                // Récupération de l'image
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Uri selectedImage = data.getData();

                // Obtention d'un curseur
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);

                // On se place sur la première colonne
                cursor.moveToFirst();

                // Affectation de l'image dans l'imageView après avoir décodé la source
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                myImageView = (ImageView) findViewById(R.id.lenna);

                bmp = BitmapFactory.decodeFile(picturePath);
                bmpSave = bmp.copy(Bitmap.Config.ARGB_8888, true);
                myImageView.setImageBitmap(bmpSave);

            } else  if (requestCode == Take_Photo) { // Si l'image est prise depuis la caméra
                String filePath = null;

                filePath = Camera_Photo_ImagePath;
                if (filePath != null) {
                    Bitmap bmp = ( new_decode(new File(filePath)));
                    myImageView.setImageBitmap(bmp);
                } else {
                    bmp = null;
                }
            }
            else {
                Toast.makeText(this, "Vous n'avez pas selectionné d'image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Un problème s'est produit", Toast.LENGTH_LONG)
                    .show();
        }



    }
    
    // fonction permettant de décoder un fichier contenant une photo prise depuis la caméra
    /* Fonction récupérée sur un forum de stackOverFlow */

    public static Bitmap new_decode(File f) {

        // decode image size

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Find the correct scale value. It should be the power of 2.
        final int REQUIRED_SIZE = 300;
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 1.5 < REQUIRED_SIZE && height_tmp / 1.5 < REQUIRED_SIZE)
                break;
            width_tmp /= 1.5;
            height_tmp /= 1.5;
            scale *= 1.5;
        }

        // decode with inSampleSize
        try {

            Bitmap bitmap= BitmapFactory.decodeStream(new FileInputStream(f), null, null);
            System.out.println(" IW " + width_tmp);
            System.out.println("IHH " + height_tmp);
            int iW = width_tmp;
            int iH = height_tmp;

            return Bitmap.createScaledBitmap(bitmap, iW, iH, true);

        } catch (OutOfMemoryError e) {
            // TODO: handle exception
            e.printStackTrace();
            System.gc();
            return null;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

    }

    public int getMax(Bitmap bmp){
        int h = bmp.getHeight();
        int w = bmp.getWidth();
        int[] pixels = new int[h * w];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);


        int pixel0 = pixels[0];
        int r0 = Color.red(pixel0);
        int g0 = Color.green(pixel0);
        int b0 = Color.blue(pixel0);
        int max = (r0+g0+b0)/3;

        for(int i = 1; i < h * w ; ++i){
            int pixel = pixels[i];
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            if((r+g+b)/3 > max){
                max = (r+g+b)/3;
            }
        }
        return max;
    }

    public void brightnessSeek(Bitmap bmp, int value) {

        // image size
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        // color information
        int A, R, G, B;
        int pixel;


        int[] pixels = new int[ width * height ];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);

        // scan through all pixels
        for(int i = 0; i < width*height; ++i) {

            // get pixel color
            pixel = pixels[i];
            A = Color.alpha(pixel);
            R = Color.red(pixel);
            G = Color.green(pixel);
            B = Color.blue(pixel);

            // increase/decrease each channel
            R += value;
            if(R > 255) { R = 255; }
            else if(R < 0) { R = 0; }

            G += value;
            if(G > 255) { G = 255; }
            else if(G < 0) { G = 0; }

            B += value;
            if(B > 255) { B = 255; }
            else if(B < 0) { B = 0; }

            pixels[i] = Color.rgb(R,G,B);

        }

        bmp.setPixels(pixels,0, width,0, 0, width, height);
        myImageView.setImageBitmap(bmp);

    }

    public void toGray(Bitmap bmp) {
        // création de la bitmap modifiable avec les valeurs de bmp (la bitmap original)
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());

        for (int x = 0; x < bmp.getWidth(); ++x) {
            for (int y = 0; y < bmp.getHeight(); ++y) {
                // prend la couleur d'un pixel
                int pixel = bmp.getPixel(x, y);
                // récupère les couleurs  R G B
                int R = Color.red(pixel);
                int G = Color.green(pixel);
                int B = Color.blue(pixel);
                // Convertit les valeurs RGB en valeur moyenne
                R = G = B = (int) (0.3 * R + 0.59 * G + 0.11 * B);
                // affecte la couleur aux pixels du bitmap
                operation.setPixel(x, y, Color.rgb(R, G, B));
            }
        }
        // affecte les changements effectués à l'image
        myImageView.setImageBitmap(operation);
    }

    public Bitmap toGray2(Bitmap bmp) {
        // création de la bitmap modifiable
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());

        int h = bmp.getHeight();
        int w = bmp.getWidth();
        int[] pixels = new int[h * w];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);
        // récupère les pixels
        for (int i = 0; i < h * w; ++i) {
            // récupère les couleurs RGB du pixel
            int r = Color.red(pixels[i]);
            int b = Color.blue(pixels[i]);
            int g = Color.green(pixels[i]);
            // calcule une moyenne de la couleur
            int moy = (int) (0.3 * r + 0.59 * g + 0.11 * b);
            // affecte cette couleur aux pixels
            pixels[i] = Color.rgb(moy, moy, moy);
        }
        operation.setPixels(pixels, 0, w, 0, 0, w, h);
        myImageView.setImageBitmap(operation);

        return operation;
    }

    public void sepia(Bitmap bmp) {
        // création de la bitmap modifiable
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());

        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int R, G, B;

        int[] pixels = new int[h*w];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);
        // Parcourt l'image
        for(int i=0;i<pixels.length;i++){
            
            R = Color.red(pixels[i]);
            G = Color.green(pixels[i]);
            B = Color.blue(pixels[i]);
            B = G = R = (int)(0.3 * R + 0.59 * G + 0.11 * B); // Griser les pixels
            // appliquer le niveau d'intensité nécessaire pour obtenir le filtre sepia à chaque canal de couleurs
            R += 94;
            if(R > 255) { R = 255; }

            G += 38;
            if(G > 255) { G = 255; }

            B += 18;
            if(B > 255) { B = 255; }

            pixels[i]= Color.rgb(R,G,B);

        }
        operation.setPixels(pixels, 0, w, 0, 0, w, h);
        myImageView.setImageBitmap(operation);
    }

    public void invert(Bitmap bmp){
        // création de la bitmap modifiable
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());

        int w = bmp.getWidth();
        int h = bmp.getHeight();

        int[] pixels = new int[h*w];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int i=0;i<pixels.length;i++){
            int R = 255-Color.red(pixels[i]);
            int G = 255-Color.green(pixels[i]);
            int B = 255-Color.blue(pixels[i]);

            pixels[i]= Color.rgb(R,G,B);
        }
        operation.setPixels(pixels, 0, w, 0, 0, w, h);
        myImageView.setImageBitmap(operation);
    }

    public void brightnessPlus(Bitmap bmp, int value) {

        // image size
        int width = bmp.getWidth();
        int height = bmp.getHeight();


        int[] pixels = new int[ width * height ];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);


        // color information
        int  R, G, B;
        int pixel;

        // scan through all pixels
        for(int i = 0; i < width*height; ++i) {

            // get pixel color
            pixel = pixels[i];

            R = Color.red(pixel);
            G = Color.green(pixel);
            B = Color.blue(pixel);

            // increase/decrease each channel
            R += value;
            if(R > 255) { R = 255; }
            else if(R < 0) { R = 0; }

            G += value;
            if(G > 255) { G = 255; }
            else if(G < 0) { G = 0; }

            B += value;
            if(B > 255) { B = 255; }
            else if(B < 0) { B = 0; }

            pixels[i] = Color.rgb(R,G,B);

        }

        bmp.setPixels(pixels,0, width,0, 0, width, height);
        myImageView.setImageBitmap(bmp);

    }

    public void brightnessMoins(Bitmap bmp, int value){

        // image size
        int width = bmp.getWidth();
        int height = bmp.getHeight();


        int[] pixels = new int[ width * height ];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);


        // color information
        int  R, G, B;
        int pixel;

        // scan through all pixels
        for(int i = 0; i < width*height; ++i) {

            // get pixel color
            pixel = pixels[i];

            R = Color.red(pixel);
            G = Color.green(pixel);
            B = Color.blue(pixel);

            // increase/decrease each channel
            R += value;
            if(R > 255) { R = 255; }
            else if(R < 0) { R = 0; }

            G += value;
            if(G > 255) { G = 255; }
            else if(G < 0) { G = 0; }

            B += value;
            if(B > 255) { B = 255; }
            else if(B < 0) { B = 0; }

            pixels[i] = Color.rgb(R,G,B);

        }

        bmp.setPixels(pixels,0, width,0, 0, width, height);
        myImageView.setImageBitmap(bmp);

    }

    public void raz(){
        myImageView = (ImageView)findViewById(R.id.lenna);
        final Bitmap b = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.lenna);
        bmp = b.copy(Bitmap.Config.ARGB_8888, true);
        bmp = bmpSave.copy(Bitmap.Config.ARGB_8888, true);
        myImageView.setImageBitmap(bmp);
        maxLum = getMax(bmp);
        maxContrast = 200;
        seekbarLuminosity.setProgress(maxLum/2);
        seekbarContrast.setProgress(maxContrast/2);

    }

    public void toColorize(Bitmap bmp) {
        // création de la bitmap modifiable
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());

        // Récupération des dimensions
        int width = bmp.getWidth();
        int height = bmp.getHeight();


        int[] pixels = new int[width * height];
        // crée une variable aléatoire
        Random ran = new Random();
        // nbr va prendre en charge les possibilités [0 ... 360) pour la teinte
        int nbr = ran.nextInt(360);


        bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

        // prend pour chaque pixel ses composantes couleurs R G B
        for (int i = 0; i < height * width; ++i) {
            int p = pixels[i];
            int r = Color.red(p);
            int g = Color.green(p);
            int b = Color.blue(p);

            float[] hsv = new float[3];

            // Changement d'espace
            Color.RGBToHSV(r, g, b, hsv);
            hsv[0] = nbr;
            hsv[1] = 1.0f;

            // Re changement d'espace puis affectatiton de la valeur au pixels i
            pixels[i] = Color.HSVToColor(hsv);
        }
        operation.setPixels(pixels, 0, width, 0, 0, width, height);
        myImageView.setImageBitmap(operation);
    }
    
    /* Fonction récupérée sur une page GitHub*/

    public void contrast(Bitmap src, double value) {


        // Taille de l'image
        int width = src.getWidth();
        int height = src.getHeight();

        // Crée un canvas qui va contenir la bitmap finale dessiné à partir de la bitmap originale
        Canvas c = new Canvas();
        c.setBitmap(src);

        // dessine la bitmap finale depuis la bitmap initiale pour qu'on puisse la modifier
        c.drawBitmap(src, 0, 0, new Paint(Color.BLACK));


        int A, R, G, B;
        int pixel;
        // Prend la valeur du contraste
        double contrast = Math.pow((100 + value) / 100, 2);

        // Parcourt l'image
        for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
                // Prend la couleur du pixel
                pixel = src.getPixel(x, y);
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                
            // Applique le filtre contraste aux trois canaux de couleurs R, G, B
                R = Color.red(pixel);
                R = (int)(((((R / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(R < 0) { R = 0; }
                else if(R > 255) { R = 255; }

                G = Color.green(pixel);
                G = (int)(((((G / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(G < 0) { G = 0; }
                else if(G > 255) { G = 255; }

                B = Color.blue(pixel);
                B = (int)(((((B / 255.0) - 0.5) * contrast) + 0.5) * 255.0);
                if(B < 0) { B = 0; }
                else if(B > 255) { B = 255; }

                // Applique le changement de couleur à la bitmap 
                src.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }
        myImageView.setImageBitmap(src);
    }

    public int[] histogram(Bitmap bmp) {
        
        Bitmap bmp2 = toGray2(bmp);
        // Grise bmp
        int w = bmp2.getWidth();
        int h = bmp2.getHeight();
        int[] hist = new int[256]; // Crée un tableau de taille 256 pour chaque niveau de gris de bmp
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int color = bmp2.getPixel(i, j) // Prend le niveau de gris du pixel
                int R = Color.red(color);
                hist[R] = hist[R] + 1; // Augmente le nombre de pixels ayant le niveau de gris correspondant 
            }
        }
        return hist;
    }
 public int[] dynamic(Bitmap bmp) {
         // Calcule les valeurs max et min de l'histogramme de bmp
         
        int[] hist = histogram(bmp);
        int[] D = new int[2];
        int min = 0;
        int max = 0;
        int maxi = hist[0];
        int mini = hist[0];
        for (int i = 0; i < hist.length; i++) {
            if (hist[i] > maxi) {
                max = i;
            } else if (hist[i] < mini) {
                min = i;
            }
        }
        D[0] = max; 
        D[1] = min;
        return D; 
    }

    public void contrast(Bitmap bmp) {
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());

        // Image size
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        
        int[] pixels = new int[h * w];
        
        int[] D = dynamic(bmp);

        bmp.getPixels(pixels, 0, w, 0, 0, w, h);
        
        // Applique l'extension linéaire de dynamique à l'image
        
        for (int i = 0; i < pixels.length; ++i) {
            int R = 255 * ((Color.red(pixels[i])) - D[1]) / (D[0] - D[1]);
            int G = 255 * ((Color.green(pixels[i])) - D[1]) / (D[0] - D[1]);
            int B = 255 * ((Color.blue(pixels[i])) - D[1]) / (D[0] - D[1]);
            pixels[i] = Color.rgb(R, G, B);
        }
        operation.setPixels(pixels, 0, w, 0, 0, w, h);
        myImageView.setImageBitmap(operation);
    }

    public void egalHistogram(Bitmap bmp){
        // création de la bitmap modifiable
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());

        // Taille de l'image
        int w = bmp.getWidth();
        int h = bmp.getHeight();

        int[] pixels= new int[h*w];
        // Calcule l'histogramme de l'image
        int[] histo = histogram(bmp);
        
        int[] C = new int[histo.length];
        C[0]=histo[0];
        // Calcule l'histogramme cumulé de l'image
        for(int i=1;i<histo.length;i++){
            C[i] = C[i-1] + histo[i];
        }
        
        // Egalise l'histogramme de l'image
        bmp.getPixels(pixels,0,w,0,0,w,h);
        for(int i=0;i<pixels.length;i++){
            int R = Color.red(pixels[i]);
            R = C[R]*255/pixels.length;
            int G = Color.green(pixels[i]);
            G = C[G]*255/pixels.length;
            int B = Color.blue(pixels[i]);
            B = C[B]*255/pixels.length;

            pixels[i]=Color.rgb(R,G,B);
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
        myImageView.setImageBitmap(bmp);
    }


    public Bitmap Intensity(Bitmap bmp){
        // Image size
        int w = bmp.getWidth();
        int h = bmp.getHeight();

        int[] pixels= new int[h*w];
        
        // Calcule de l'intensité de bmp

        for (int i=0;i<pixels.length;i++){
            int R = Color.red(pixels[i]);
            int G = Color.green(pixels[i]);
            int B = Color.blue(pixels[i]);
            int color = (R + G + B)/3; //Intensité du pixel i
            pixels[i]= Color.rgb(color,color,color);

        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
        myImageView.setImageBitmap(bmp);

        return bmp;

    }

    public void egalImgCouleur(Bitmap bmp){
        //Calcule l'intensité de bmp
        bmp = Intensity(bmp);
        // Image size
        int w = bmp.getWidth();
        int h = bmp.getHeight();

        int[] pixels= new int[h*w];
        int[] histo = histogram(bmp);
        int[] C = new int[histo.length];
        C[0]=histo[0];

        for(int i=1;i<histo.length;i++){
            C[i] = C[i-1] + histo[i];
        }
        bmp.getPixels(pixels,0,w,0,0,w,h);
        for(int i=0;i<pixels.length;i++){
            int R = Color.red(pixels[i]);
            R = C[R]*255/pixels.length;
            int G = Color.green(pixels[i]);
            G = C[G]*255/pixels.length;
            int B = Color.blue(pixels[i]);
            B = C[B]*255/pixels.length;

            pixels[i]=Color.rgb(R,G,B);
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
        myImageView.setImageBitmap(bmp);

    }

    public void convolution(Bitmap bmp){

        // création de la bitmap modifiable avec les valeurs de bmp (la bitmap original)
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());

        int SIZE = 3;


        int [][] Matrix = new int[SIZE][SIZE];
        for(int i = 0; i < SIZE; ++i){
            for(int j = 0; j < SIZE; ++j){
                Matrix[i][j] = 1;
            }
        }



        int width = bmp.getWidth();
        int height = bmp.getHeight();

        int sumR, sumG ,sumB = 0;



        for(int x = 1; x < width-1 ; ++x) {
            for(int y = 1 ; y < height-1 ; ++y) {

                sumR = sumG = sumB = 0;


                for(int u = -1; u <= 1; ++u){
                    for(int v= -1; v <= 1; ++v){

                        sumR += Color.red(bmp.getPixel(x + u, y + v)) * Matrix[u+1][v+1];
                        sumG += Color.green(bmp.getPixel(x + u, y + v)) * Matrix[u+1][v+1];
                        sumB += Color.blue(bmp.getPixel(x + u, y + v)) * Matrix[u+1][v+1];
                    }
                }


                sumR = sumR / 9;

                sumG = sumG / 9;

                sumB = sumB / 9;


                operation.setPixel(x,y, Color.rgb(sumR,sumG,sumB));

            }
        }

        myImageView.setImageBitmap(operation);
    }
    
    // Partie concernant l'implémentation du zoom.
        /* Fonction récupérée sur stackOverFlow */
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        ImageView view = (ImageView) v;
        view.setScaleType(ImageView.ScaleType.MATRIX);
        float scale;

        dumpEvent(event);
        // Handle touch events here...

        switch (event.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:   // first finger down only
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                Log.d(TAG, "mode=DRAG"); // write to LogCat
                mode = DRAG;
                break;

            case MotionEvent.ACTION_UP: // first finger lifted

            case MotionEvent.ACTION_POINTER_UP: // second finger lifted

                mode = NONE;
                Log.d(TAG, "mode=NONE");
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // first and second finger down

                oldDist = spacing(event);
                Log.d(TAG, "oldDist=" + oldDist);
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                    Log.d(TAG, "mode=ZOOM");
                }
                break;

            case MotionEvent.ACTION_MOVE:

                if (mode == DRAG)
                {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y); // create the transformation in the matrix  of points
                }
                else if (mode == ZOOM)
                {
                    // pinch zooming
                    float newDist = spacing(event);
                    Log.d(TAG, "newDist=" + newDist);
                    if (newDist > 5f)
                    {
                        matrix.set(savedMatrix);
                        scale = newDist / oldDist; // setting the scaling of the
                        // matrix...if scale > 1 means
                        // zoom in...if scale < 1 means
                        // zoom out
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        view.setImageMatrix(matrix); // display the transformation on screen

        return true; // indicate event was handled
    }

    /*
     * --------------------------------------------------------------------------
     * Method: spacing Parameters: MotionEvent Returns: float Description:
     * checks the spacing between the two fingers on touch
     * ----------------------------------------------------
     */

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        float F = (x*x+y*y)*(x*x+y*y);
        return F;
    }

    /*
     * --------------------------------------------------------------------------
     * Method: midPoint Parameters: PointF object, MotionEvent Returns: void
     * Description: calculates the midpoint between the two fingers
     * ------------------------------------------------------------
     */

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /** Show an event in the LogCat view, for debugging */
    private void dumpEvent(MotionEvent event) {
        String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE","POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
        StringBuilder sb = new StringBuilder();
        int action = event.getAction();
        int actionCode = action & MotionEvent.ACTION_MASK;
        sb.append("event ACTION_").append(names[actionCode]);

        if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP)
        {
            sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
            sb.append(")");
        }

        sb.append("[");
        for (int i = 0; i < event.getPointerCount(); i++)
        {
            sb.append("#").append(i);
            sb.append("(pid ").append(event.getPointerId(i));
            sb.append(")=").append((int) event.getX(i));
            sb.append(",").append((int) event.getY(i));
            if (i + 1 < event.getPointerCount())
                sb.append(";");
        }

        sb.append("]");
        Log.d("Touch Events ---------", sb.toString());
    }

}
