import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleLogger {
    private boolean mLoggingEnabled=true;
    private String mTAG="";

    SimpleLogger() {

    }

    SimpleLogger(boolean LoggingEnabled) {
        mLoggingEnabled=LoggingEnabled;
    }

    SimpleLogger(String TAG) {
        mTAG=TAG;
    }

    SimpleLogger(String TAG, boolean LoggingEnabled) {
        mTAG=TAG;
        mLoggingEnabled=LoggingEnabled;
    }

    public void enableLogging(boolean enable){
        mLoggingEnabled=enable;
    }

    private  String dt(){
        String strDate ="";
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");//dd/MM/yyyy
            Date now = new Date();
            strDate = sdfDate.format(now);
        }catch (Exception e){
            strDate="1970-01-01 00:00:00:000";
        }finally {
            return strDate + " - ";
        }
    }

    public void log(String info){
        if (!mLoggingEnabled){return;}
        System.out.println(dt() + mTAG + info);
    }

    public void log(String TAG,String info){
        if (!mLoggingEnabled){return;}
        System.out.println(dt()+ TAG + "->" + info);
    }
}
