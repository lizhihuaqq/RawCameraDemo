package cc.uphoton.rawcamera.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static byte[] getBytesByFile(String pathStr) {
        File file = new File(pathStr);
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            byte[] data = bos.toByteArray();
            bos.close();
            return data;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 删除单个文件
     * @param   filePath    被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    public static boolean  deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }
    //镜像NV21图片
    public static byte[] NV21_mirror(byte[] nv21_data, int width, int height) {
        int i;
        int left, right;
        byte temp;
        int startPos = 0;

// mirror Y
        for (i = 0; i < height; i++) {
            left = startPos;
            right = startPos + width - 1;
            while (left < right) {
                temp = nv21_data[left];
                nv21_data[left] = nv21_data[right];
                nv21_data[right] = temp;
                left++;
                right--;
            }
            startPos += width;
        }


// mirror U and V
        int offset = width * height;
        startPos = 0;
        for (i = 0; i < height / 2; i++) {
            left = offset + startPos;
            right = offset + startPos + width - 2;
            while (left < right) {
                temp = nv21_data[left];
                nv21_data[left] = nv21_data[right];
                nv21_data[right] = temp;
                left++;
                right--;

                temp = nv21_data[left];
                nv21_data[left] = nv21_data[right];
                nv21_data[right] = temp;
                left++;
                right--;
            }
            startPos += width;
        }
        return nv21_data;
    }
    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyy-MM-dd HH:mm:ss.SSS
     */
    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        String dateString = formatter.format(currentTime);

        return dateString;
    }
}
