package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.example.baidu.asrdemo.Base64Util;
import com.example.baidu.common.ConnUtil;
import com.example.baidu.common.DemoException;
import com.example.baidu.common.TokenHolder;
import com.example.test.R;
import com.example.test.utility.PcmToWavUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class SecordActivity extends AppCompatActivity {
    private int mRecordBufferSize;

    private void initMinBufferSize()
    {
        //获取每一帧的字节流大小
        this.mRecordBufferSize = AudioRecord.getMinBufferSize(16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    private AudioRecord mAudioRecord;

    private void initAudioRecord()
    {
        this.mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
//		                                    音频源选择使用麦克风
                16000,
                AudioFormat.CHANNEL_IN_MONO,
//		                                    声道配置：单声道
                AudioFormat.ENCODING_PCM_16BIT,
                this.mRecordBufferSize);
//											音频数据格式
    }

    private boolean mWhetherRecord;

    private File pcmFile;

    private void startRecord()
    {
        this.pcmFile        = new File(Objects.requireNonNull(SecordActivity.this.getExternalCacheDir()).getPath(),
                "audioRecord.pcm");
        this.mWhetherRecord = true;
//		线程
        new Thread(() ->
        {
            this.mAudioRecord.startRecording(); // 开始录制
            FileOutputStream fileOutputStream;
            try
            {
                fileOutputStream = new FileOutputStream(this.pcmFile);
                byte[] bytes = new byte[this.mRecordBufferSize];
                while (this.mWhetherRecord)
                {
                    this.mAudioRecord.read(bytes, 0, bytes.length); // 读取流
                    fileOutputStream.write(bytes);
                    fileOutputStream.flush();
                }
                this.mAudioRecord.stop(); // 停止录制
                fileOutputStream.flush();
                fileOutputStream.close();
                this.addHeadData(); // 添加音频头部信息并且转成wav格式
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
                this.mAudioRecord.stop();
            }
            catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void stopRecord() { this.mWhetherRecord = false; }
//	切换布尔值就可以关闭音频录制

    private void addHeadData()
//			给音频文件添加头部信息,并且转换格式成wav
    {
        this.pcmFile = new File(Objects.requireNonNull(SecordActivity.this.getExternalCacheDir()).getPath(),
                "audioRecord.pcm");
        File handlerWavFile = new File(SecordActivity.this.getExternalCacheDir().getPath(),
                "audioRecord_handler.wav");
        PcmToWavUtil pcmToWavUtil = new PcmToWavUtil(16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        pcmToWavUtil.pcmToWav(this.pcmFile.toString(), handlerWavFile.toString());
    }


    // 播放部分，将来要去除

    private boolean recordFlag = true;

    private MediaPlayer mediaPlayer = new MediaPlayer();

    private void initMediaPlayer()
    {
        // 使用MediaPlayer加载指定的声音文件。
        try
        {
            this.mediaPlayer
                    .setDataSource(new File(Objects.requireNonNull(SecordActivity.this.getExternalCacheDir()).getPath(),
                            "audioRecord_handler.wav")
                            .getPath());
            this.mediaPlayer.prepare(); // 让MediaPlayer进入到准备状态
        }
        catch (IOException e) { e.printStackTrace(); }
    }



    // 百度语音识别部分

    private final boolean METHOD_RAW = true; // 默认以json方式上传音频文件

    //  填写网页上申请的appkey 如 $apiKey="g8eBUMSokVB1BHGmgxxxxxx"
    private final String APP_KEY = "DuRjBE0N0drOeHb271zekxQu";

    // 填写网页上申请的APP SECRET 如 $SECRET_KEY="94dc99566550d87f8fa8ece112xxxxx"
    private final String SECRET_KEY = "WFgi0jUwmBvLWalG0qlMfdi4sWigGtcm";

    // 需要识别的文件
    private final String FILENAME = "16k.pcm";

    // 文件格式, 支持pcm/wav/amr 格式，极速版额外支持m4a 格式
    private final String FORMAT = "wav";

    private String CUID = "1234567JAVA";

    // 采样率固定值
    private final int RATE = 16000;

    private String URL;

    private int DEV_PID;

    //private int LM_ID;//测试自训练平台需要打开此注释

    private String SCOPE;

    //  普通版 参数
    {
        URL = "https://vop.baidu.com/server_api"; // 可以改为https
        //  1537 表示识别普通话，使用输入法模型。 其它语种参见文档
        DEV_PID = 1537;
        SCOPE   = "audio_voice_assistant_get";
    }

    public String run() throws IOException, DemoException
    {
        TokenHolder holder = new TokenHolder(APP_KEY, SECRET_KEY, SCOPE);
        holder.resfresh();
        String token  = holder.getToken();
        String result = null;
        if (METHOD_RAW)
        {
            result = runRawPostMethod(token);
        }
        else
        {
            result = runJsonPostMethod(token);
        }
        return result;
    }

    private String runRawPostMethod(String token) throws IOException, DemoException
    {
        String url2 = URL + "?cuid=" + ConnUtil.urlEncode(CUID) + "&dev_pid=" + DEV_PID + "&token=" + token;
        //测试自训练平台需要打开以下信息
        //String url2 = URL + "?cuid=" + ConnUtil.urlEncode(CUID) + "&dev_pid=" + DEV_PID + "&lm_id="+ LM_ID +
        // "&token=" + token;
        String contentTypeStr = "audio/" + FORMAT + "; rate=" + RATE;
        //System.out.println(url2);
        byte[]            content = getFileContent(FILENAME);
        HttpURLConnection conn    = (HttpURLConnection) new URL(url2).openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("Content-Type", contentTypeStr);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream().write(content);
        conn.getOutputStream().close();
        System.out.println("url is " + url2);
        System.out.println("header is  " + "Content-Type :" + contentTypeStr);
        String result = ConnUtil.getResponseString(conn);
        return result;
    }

    public String runJsonPostMethod(String token) throws DemoException, IOException
    {

        byte[]     content = getFileContent(FILENAME);
        String     speech  = base64Encode(content);
        String     result  = null;
        JSONObject params  = new JSONObject();
        try
        {
            params.put("dev_pid", DEV_PID);

            //params.put("lm_id",LM_ID);//测试自训练平台需要打开注释
            params.put("format", FORMAT);
            params.put("rate", RATE);
            params.put("token", token);
            params.put("cuid", CUID);
            params.put("channel", "1");
            params.put("len", content.length);
            params.put("speech", speech);

            // System.out.println(params.toString());
            HttpURLConnection conn = (HttpURLConnection) new URL(URL).openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.getOutputStream().write(params.toString().getBytes());
            conn.getOutputStream().close();
            result = ConnUtil.getResponseString(conn);


            params.put("speech", "base64Encode(getFileContent(FILENAME))");

            System.out.println("url is : " + URL);
            System.out.println("params is :" + params.toString());

        }
        catch (JSONException e) { e.printStackTrace(); }
        return result;
    }

    private byte[] getFileContent(String filename) throws DemoException, IOException
    {
        File file = new File(Objects.requireNonNull(SecordActivity.this.getExternalCacheDir()).getPath(),
                "audioRecord_handler.wav");
        if (!file.canRead())
        {
            System.err.println("文件不存在或者不可读: " + file.getAbsolutePath());
            throw new DemoException("file cannot read: " + file.getAbsolutePath());
        }
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(file);
            return ConnUtil.getInputStreamContent(is);
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }

    private String base64Encode(byte[] content)
    {
        /**
         Base64.Encoder encoder = Base64.getEncoder(); // JDK 1.8  推荐方法
         String str = encoder.encodeToString(content);
         **/

        char[] chars = Base64Util.encode(content); // 1.7 及以下，不推荐，请自行跟换相关库
        String str   = new String(chars);

        return str;
    }

    private void showResponse(final String response)
    {
        this.runOnUiThread(() ->
        {
            EditText editText = this.findViewById(R.id.output);
            editText.setText(response);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secord);
        // 提交后跳转
        Button btn1 = findViewById(R.id.uploading);
        btn1.setOnClickListener(v ->
        {
            Intent i = new Intent(SecordActivity.this, PhotoActivity.class);
            startActivity(i);
        });

        // 百度语音识别部分
        this.initMinBufferSize();
        this.initAudioRecord();
        EditText editText        = this.findViewById(R.id.output);
        Button   recordingButton = this.findViewById(R.id.recordingButton);
        recordingButton.setOnClickListener(
                v ->
                {
                    if (this.recordFlag)
                    {
                        this.startRecord();
                        recordingButton.setText("点击停止");
                        this.recordFlag = false;
                    }
                    else
                    {
                        this.stopRecord();
                        recordingButton.setText("点击说话");
                        this.recordFlag = true;

                        new Thread(
                                () ->
                                {
                                    String result = null;
                                    try
                                    {
                                        result = this.run();
                                    }
                                    catch (IOException | DemoException e) { e.printStackTrace(); }
                                    this.showResponse(result);
                                }).start();
                    }
                });

        // 播放录音按钮，将来要去除，现存只能点击一次的问题，没必要改
        Button audioPlayButton = this.findViewById(R.id.audioPlay);
        audioPlayButton.setOnClickListener(v ->
        {
            this.initMediaPlayer();
            this.mediaPlayer.start();
        });

        // 点击跳转主页面
        Button btn2 = findViewById(R.id.home);
        btn2.setOnClickListener(v ->
        {
            Intent i = new Intent(SecordActivity.this, MainActivity.class);
            startActivity(i);
        });
    }
}