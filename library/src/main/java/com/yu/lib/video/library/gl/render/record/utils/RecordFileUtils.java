package com.yu.lib.video.library.gl.render.record.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordFileUtils {

    public static String getTempFilePath(String dirPath, int segmentIndex) {
        return dirPath + File.separator + "yu_video_temp_" + segmentIndex + ".mp4";
    }

    public static List<String> getTempSegmentFiles(String dirPath, int curSegmentIndex) {
        List<String> list = new ArrayList<>();
        for (int i=0; i<=curSegmentIndex; i++) {
            File file = new File(getTempFilePath(dirPath, i));
            if(file.exists()) {
                list.add(file.getAbsolutePath());
            }
        }
        return list;
    }

    public static void deleteAllTempSegmentVideoFile(String dirPath, int curSegmentIndex) {
        for (int i=0; i<=curSegmentIndex; i++) {
            File file = new File(getTempFilePath(dirPath, i));
            if(file.exists()) {
                file.delete();
            }
        }
    }

    public static boolean deleteRecordFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

}
