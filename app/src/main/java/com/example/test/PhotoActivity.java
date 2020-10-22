package com.example.test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class PhotoActivity extends AppCompatActivity {
    public static final int TAKE_PHOTO=1;
    private ImageView picture;
    private Uri imageUri;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        Button gomain=(Button)findViewById(R.id.goMain);
        gomain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(PhotoActivity.this,MainActivity.class);
                startActivity(i);
            }
        });


        final Button takephoto = (Button)findViewById(R.id.takephoto);
        picture =findViewById(R.id.picture);
        mContext=PhotoActivity.this;





        takephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File outputImage=new File(getExternalCacheDir(),"output_image.jpg");//创建jpg文件用于存储
                try{

                    if(outputImage.exists()){
                        outputImage.delete();//如果存在就删除原文件
                    }outputImage.createNewFile();//不存在就创建新的
                }catch(IOException e){
                    e.printStackTrace();//用于显示IO报错
                }

                if(Build.VERSION.SDK_INT>=24){//版本号大于7.0
                    imageUri = FileProvider.getUriForFile(PhotoActivity.this,"IHEBUT",outputImage);//创建路径，作者
                }else{
                    imageUri=Uri.fromFile(outputImage);//照片直接存储
                }

                if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.CAMERA}, 100);//动态获取相机权限
                }else{
                    startCamera();//打开相机
                }
            }
        });


    }
    private void startCamera(){
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent,TAKE_PHOTO);
    }
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (requestCode == RESULT_OK*(-1)) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        picture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode) {
            case 100:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 启动相机程序
                    startCamera();
                } else {
                    Toast.makeText(mContext, "没有权限", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }



    }
}